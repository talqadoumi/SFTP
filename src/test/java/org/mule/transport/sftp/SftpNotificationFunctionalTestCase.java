/*
 * $Id$
 * --------------------------------------------------------------------------------------
 * Copyright (c) MuleSource, Inc.  All rights reserved.  http://www.mulesource.com
 *
 * The software in this package is published under the terms of the MPL style
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */


package org.mule.transport.sftp;

import org.mule.transport.sftp.notification.SftpTransportNotificationTestListener;

/**
 * Test the notification features.
 */
public class SftpNotificationFunctionalTestCase extends AbstractSftpTestCase
{
	private static final long TIMEOUT = 15000;

	// Size of the generated stream - 2 Mb
	final static int SEND_SIZE = 1024 * 1024 * 2;

	@Override
	protected void doSetUp() throws Exception {
		super.doSetUp();
		SftpTransportNotificationTestListener.reset();
	}

	protected String getConfigResources()
	{
		return "mule-sftp-notification-test-config.xml";
	}

	/**
	 * Test notification.
	 */
	public void testNotification() throws Exception
	{
		executeBaseTest("inboundEndpoint", "vm://test.upload", "file.txt", SEND_SIZE, "receiving", TIMEOUT);
	}

	/**
	 * To be overridden by the test-classes if required
	 */
	@Override
	protected void executeBaseAssertionsBeforeCall() {
		
		super.executeBaseAssertionsBeforeCall();
		
		//  Assert that none of the sftp-notifications already are set
		assertFalse(SftpTransportNotificationTestListener.gotSftpPutNotification());
		assertFalse(SftpTransportNotificationTestListener.gotSftpRenameNotification());
		assertFalse(SftpTransportNotificationTestListener.gotSftpGetNotification());
		assertFalse(SftpTransportNotificationTestListener.gotSftpDeleteNotification());		
	}	

	/**
	 * To be overridden by the test-classes if required
	 */
	@Override
	protected void executeBaseAssertionsAfterCall(int sendSize, int receivedSize) {
		
		super.executeBaseAssertionsAfterCall(sendSize, receivedSize);

		//  Now also verify that we got the expected sftp-notifications
		assertTrue(SftpTransportNotificationTestListener.gotSftpPutNotification());
		assertTrue(SftpTransportNotificationTestListener.gotSftpRenameNotification());
		assertTrue(SftpTransportNotificationTestListener.gotSftpGetNotification());
		assertTrue(SftpTransportNotificationTestListener.gotSftpDeleteNotification());
	}
}