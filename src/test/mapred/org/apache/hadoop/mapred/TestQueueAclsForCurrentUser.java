/**
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.apache.hadoop.mapred;

import java.io.IOException;
import javax.security.auth.login.LoginException;
import junit.framework.TestCase;
import org.apache.hadoop.security.UserGroupInformation;

/**
 * Unit test class to test queue acls
 *
 */
public class TestQueueAclsForCurrentUser extends TestCase {

  private QueueManager queueManager;
  private JobConf conf = null;
  UserGroupInformation currentUGI = null;
  String submitAcl = Queue.QueueOperation.SUBMIT_JOB.getAclName();
  String adminAcl  = Queue.QueueOperation.ADMINISTER_JOBS.getAclName();

  private void setupConfForNoAccess() throws IOException,LoginException {
    currentUGI = UserGroupInformation.getLoginUser();
    String userName = currentUGI.getUserName();
    conf = new JobConf();

    conf.setBoolean("mapred.acls.enabled",true);

    conf.set("mapred.queue.names", "qu1,qu2");
    //Only user u1 has access
    conf.set("mapred.queue.qu1.acl-submit-job", "u1");
    conf.set("mapred.queue.qu1.acl-administer-jobs", "u1");
    //q2 only group g2 has acls for the queues
    conf.set("mapred.queue.qu2.acl-submit-job", " g2");
    conf.set("mapred.queue.qu2.acl-administer-jobs", " g2");
    queueManager = new QueueManager(conf);

  }

  /**
   *  sets up configuration for acls test.
   * @return
   */
  private void setupConf(boolean aclSwitch) throws IOException,LoginException{
    currentUGI = UserGroupInformation.getLoginUser();
    String userName = currentUGI.getUserName();
    conf = new JobConf();

    conf.setBoolean("mapred.acls.enabled", aclSwitch);

    conf.set("mapred.queue.names", "qu1,qu2,qu3,qu4,qu5,qu6,qu7");
    //q1 Has acls for all the users, supports both submit and administer
    conf.set("mapred.queue.qu1.acl-submit-job", "*");
    conf.set("mapred.queue.qu1-acl-administer-jobs", "*");
    //q2 only u2 has acls for the queues
    conf.set("mapred.queue.qu2.acl-submit-job", "u2");
    conf.set("mapred.queue.qu2.acl-administer-jobs", "u2");
    //q3  Only u2 has submit operation access rest all have administer access
    conf.set("mapred.queue.qu3.acl-submit-job", "u2");
    conf.set("mapred.queue.qu3.acl-administer-jobs", "*");
    //q4 Only u2 has administer access , anyone can do submit
    conf.set("mapred.queue.qu4.acl-submit-job", "*");
    conf.set("mapred.queue.qu4.acl-administer-jobs", "u2");
    //qu6 only current user has submit access
    conf.set("mapred.queue.qu6.acl-submit-job",userName);
    conf.set("mapred.queue.qu6.acl-administrator-jobs","u2");
    //qu7 only current user has administrator access
    conf.set("mapred.queue.qu7.acl-submit-job","u2");
    conf.set("mapred.queue.qu7.acl-administrator-jobs",userName);
    //qu8 only current group has access
    StringBuilder groupNames = new StringBuilder("");
    String[] ugiGroupNames = currentUGI.getGroupNames();
    int max = ugiGroupNames.length-1;
    for(int j=0;j< ugiGroupNames.length;j++) {
      groupNames.append(ugiGroupNames[j]);
      if(j<max) {
        groupNames.append(",");
      }
    }
    conf.set("mapred.queue.qu5.acl-submit-job"," "+groupNames.toString());
    conf.set("mapred.queue.qu5.acl-administrator-jobs"," "
            +groupNames.toString());

    queueManager = new QueueManager(conf);
  }

  public void testQueueAclsForCurrentuser() throws IOException,LoginException {
    setupConf(true);
    QueueAclsInfo[] queueAclsInfoList =
            queueManager.getQueueAcls(currentUGI);
    checkQueueAclsInfo(queueAclsInfoList);
  }

  public void testQueueAclsForCurrentUserAclsDisabled() throws IOException,
          LoginException {
    setupConf(false);
    //fetch the acls info for current user.
    QueueAclsInfo[] queueAclsInfoList = queueManager.
            getQueueAcls(currentUGI);
    checkQueueAclsInfo(queueAclsInfoList);
  }

  public void testQueueAclsForNoAccess() throws IOException,LoginException {
    setupConfForNoAccess();
    QueueAclsInfo[] queueAclsInfoList = queueManager.
            getQueueAcls(currentUGI);
    assertTrue(queueAclsInfoList.length == 0);
  }

  private void checkQueueAclsInfo(QueueAclsInfo[] queueAclsInfoList)
          throws IOException {
    if (conf.get("mapred.acls.enabled").equalsIgnoreCase("true")) {
      for (int i = 0; i < queueAclsInfoList.length; i++) {
        QueueAclsInfo acls = queueAclsInfoList[i];
        String queueName = acls.getQueueName();
        assertFalse(queueName.contains("qu2"));
        if (queueName.equals("qu1")) {
          assertTrue(acls.getOperations().length == 2);
          assertTrue(checkAll(acls.getOperations()));
        } else if (queueName.equals("qu3")) {
          assertTrue(acls.getOperations().length == 1);
          assertTrue(acls.getOperations()[0].equalsIgnoreCase(adminAcl));
        } else if (queueName.equals("qu4")) {
          assertTrue(acls.getOperations().length == 1);
          assertTrue(acls.getOperations()[0].equalsIgnoreCase(submitAcl));
        } else if (queueName.equals("qu5")) {
          assertTrue(acls.getOperations().length == 2);
          assertTrue(checkAll(acls.getOperations()));
        } else if(queueName.equals("qu6")) {
          assertTrue(acls.getOperations()[0].equals(submitAcl));
        } else if(queueName.equals("qu7")) {
          assertTrue(acls.getOperations()[0].equals(adminAcl));
        } 
      }
    } else {
      for (int i = 0; i < queueAclsInfoList.length; i++) {
        QueueAclsInfo acls = queueAclsInfoList[i];
        String queueName = acls.getQueueName();
        assertTrue(acls.getOperations().length == 2);
        assertTrue(checkAll(acls.getOperations()));
      }
    }
  }

  private boolean checkAll(String[] operations){
    boolean submit = false;
    boolean admin = false;

    for(String val: operations){
      if(val.equalsIgnoreCase(submitAcl))
        submit = true;
      else if(val.equalsIgnoreCase(adminAcl))
        admin = true;
    }
    if(submit && admin) return true;
    return false;
  }
}
