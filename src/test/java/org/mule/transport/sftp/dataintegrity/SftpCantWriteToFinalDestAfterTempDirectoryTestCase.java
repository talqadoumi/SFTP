package org.mule.transport.sftp.dataintegrity;

import org.mule.api.endpoint.ImmutableEndpoint;
import org.mule.module.client.MuleClient;
import org.mule.transport.sftp.SftpClient;

/**
 * Tests that files are not deleted if the final destination is not writable when using a
 * temp directory.
 */
public class SftpCantWriteToFinalDestAfterTempDirectoryTestCase extends AbstractSftpDataIntegrityTestCase
{

	private static String INBOUND_ENDPOINT_NAME = "inboundEndpoint";
	private static String OUTBOUND_ENDPOINT_NAME = "outboundEndpoint";

	@Override
	protected String getConfigResources()
	{
		return "dataintegrity/sftp-dataintegrity-common-with-tempdir-config.xml";
	}

     @Override
    protected void doSetUp() throws Exception {
        super.doSetUp();

         // Delete the in & outbound directories
         initEndpointDirectory(INBOUND_ENDPOINT_NAME);
         initEndpointDirectory(OUTBOUND_ENDPOINT_NAME);
    }

	/**
	 * No write access on the outbound directory but write access to the TEMP directory.
	 * The source file should still exist and no file should exist in the TEMP directory.
	 */
	public void testCantWriteToFinalDestAfterTempDirectory() throws Exception
	{
		MuleClient muleClient = new MuleClient();

		// Must create the temp directory before we change the access rights
		createRemoteDirectory(muleClient, OUTBOUND_ENDPOINT_NAME, "uploading");

        SftpClient sftpClient = getSftpClient(muleClient, OUTBOUND_ENDPOINT_NAME);

        try {
		// change the chmod to "dr-x------" on the outbound-directory
		remoteChmod(muleClient, sftpClient, OUTBOUND_ENDPOINT_NAME, 00500);

		// Send an file to the SFTP server, which the inbound-outboundEndpoint then can pick up
		muleClient.dispatch(getAddressByEndpoint(muleClient, INBOUND_ENDPOINT_NAME), TEST_MESSAGE, fileNameProperties);

		// TODO dont know any better way to wait for the above to finish? We cant use the same as SftpFileAgeFunctionalTestCase
		//   for example since we dont have the TestComponent
		Thread.sleep(4000);

		verifyInAndOutFiles(muleClient, INBOUND_ENDPOINT_NAME, OUTBOUND_ENDPOINT_NAME, true, false);

            ImmutableEndpoint endpoint = (ImmutableEndpoint) muleClient.getProperty(OUTBOUND_ENDPOINT_NAME);
            assertFalse("The inbound file should not be left in the TEMP-dir", super.verifyFileExists(sftpClient, endpoint.getEndpointURI().getPath() + "/" + TEMP_DIR, FILE_NAME));
        } finally {
            sftpClient.disconnect();
        }
    }

}
