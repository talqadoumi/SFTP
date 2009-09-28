package org.mule.transport.sftp;

import java.io.InputStream;

import org.mule.DefaultMuleMessage;
import org.mule.api.MuleMessage;
import org.mule.api.endpoint.InboundEndpoint;
import org.mule.api.transport.MessageAdapter;
import org.mule.transport.AbstractMessageRequester;
import org.mule.transport.sftp.notification.SftpNotifier;

/**
 * <code>SftpMessageRequester</code> polls files on request (e.g. from a quartz-inbound-endpoint)
 * from an sftp service on request using jsch. This requester produces an InputStream payload, which can
 * be materialized in a MessageDispatcher or Component.
 */
public class SftpMessageRequester extends AbstractMessageRequester {

    private SftpReceiverRequesterUtil sftpRRUtil = null;

    public SftpMessageRequester(InboundEndpoint endpoint) {
		super(endpoint);

        sftpRRUtil = new SftpReceiverRequesterUtil(endpoint);

	}

	@Override
	protected MuleMessage doRequest(long timeout) throws Exception {
		String[] files = sftpRRUtil.getAvailableFiles(true);
		
		if (files.length == 0) return null;
		
		String path = files[0];
		// TODO. ML FIX. Can't we figure out the current service (for logging/audit purpose)???
		SftpNotifier notifier = new SftpNotifier((SftpConnector)connector, new DefaultMuleMessage(null), endpoint, endpoint.getName());

		InputStream inputStream = sftpRRUtil.retrieveFile( path, notifier );

        logger.debug( "Routing file: " + path );

        MessageAdapter msgAdapter = connector.getMessageAdapter( inputStream );
        msgAdapter.setProperty( SftpConnector.PROPERTY_ORIGINAL_FILENAME, path );
        MuleMessage message = new DefaultMuleMessage( msgAdapter );

        // Now we can update the notifier with the message
        notifier.setMessage(message);
        return message;
	}

}
