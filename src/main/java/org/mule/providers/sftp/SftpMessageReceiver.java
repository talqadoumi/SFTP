/*
 * $Id$
 * --------------------------------------------------------------------------------------
 * Copyright (c) MuleSource, Inc.  All rights reserved.  http://www.mulesource.com
 *
 * The software in this package is published under the terms of the MuleSource MPL
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.providers.sftp;


import java.io.FilenameFilter;
import java.io.InputStream;
import java.util.ArrayList;

import org.mule.impl.MuleMessage;
import org.mule.providers.PollingMessageReceiver;
import org.mule.umo.UMOComponent;
import org.mule.umo.UMOMessage;
import org.mule.umo.endpoint.UMOEndpoint;
import org.mule.umo.lifecycle.InitialisationException;
import org.mule.umo.provider.UMOMessageAdapter;


/**
 * <code>SftpMessageReceiver</code> polls and receives files from an sftp service using 
 * jsch.  This receiver produces an InputStream payload, which can be materialized in a 
 * MessageDispatcher or Component.   
 */
public class SftpMessageReceiver extends PollingMessageReceiver
{

    //private String fileExtension;
    protected final FilenameFilter filenameFilter;

    public SftpMessageReceiver(org.mule.providers.sftp.SftpConnector connector, UMOComponent component,
                       UMOEndpoint endpoint)
            throws InitialisationException
    {
        //super(connector, component, endpoint, connector.getTrigger());
        super(connector, component, endpoint, new Long(((SftpConnector)connector)
                .getPollingFrequency()));
        
        if (endpoint.getFilter() instanceof FilenameFilter)
        {
            this.filenameFilter = (FilenameFilter)endpoint.getFilter();
        }
        else
        {
            this.filenameFilter = null;
        }
    }

    public void poll() throws Exception
    {
        
        String[] files = getAvailableFiles();
        
        if( files.length == 0 )
        {
        	logger.info("No matching files found");
        }
        
        for (int i = 0; i < files.length; i++)
        {
        	routeFile(files[i]);   

        }        
        

    }

    


    public void doConnect() throws Exception
    {
       //no op
    }

    public void doDisconnect() throws Exception
    {
       //no op
    }

    
    //Get files in directory configured on the endpoint
    protected String[] getAvailableFiles() throws Exception
    {
    	//This sftp client instance is only for checking available files.  This instance cannot be shared
    	//with clients that retrieve files because of thread safety
    	
    	//TODO: Get instance from a pool
    	SftpClient client = null;
    	
    	try
    	{
    		client = ((SftpConnector) connector).createSftpClient(endpoint.getEndpointURI());
        	String[] files = client.listFiles();    	
        	
        	//Only return files that have completely been written and match fileExtension
        	ArrayList completedFiles = new ArrayList(files.length);
        	
        	for(int i = 0; i < files.length; i++)
        	{
        		//Skip if no match
                if (filenameFilter != null && !filenameFilter.accept(null, files[i]))
                {
                    continue;
                }

        		//See if the file is still growing, leave it alone if it is
                if( !hasChanged(files[i],client))
                {
                	completedFiles.add(files[i]);
                }
                
        	}
 
        	return (String[]) completedFiles.toArray(new String[]{});
    	}
    	catch(Exception e)
    	{
	        throw e;
    	}
    	finally
    	{
        	client.disconnect();   	   		
    	}
       

    	


        
    }
 
    protected InputStream retrieveFile(String fileName) throws Exception
    {
    	
    	SftpConnector sftpConnector = (SftpConnector) connector;
    	
    	//Getting a new SFTP client dedicated to the SftpInputStream below
    	SftpClient client = sftpConnector.createSftpClient(endpoint.getEndpointURI());
    	
    	//This special InputStream closes the SftpClient when the stream is closed.
    	//The stream will be materialized in a Message Dispatcher or Service Component
    	return new SftpInputStream(client,fileName,sftpConnector.isAutoDelete());

    	
    }
    

    

    
    protected void routeFile(String path) throws Exception
    {
    	InputStream inputStream = retrieveFile(path);
    	
    	logger.debug("Routing file: " + path);
  	
        UMOMessageAdapter msgAdapter = connector.getMessageAdapter(inputStream);
        msgAdapter.setProperty(SftpConnector.PROPERTY_ORIGINAL_FILENAME, path);
        UMOMessage message = new MuleMessage(msgAdapter);
        routeMessage(message, endpoint.isSynchronous());
        
    }

    

    protected boolean hasChanged(String fileName,SftpClient client) throws Exception
    {
    	//Take consecutive file size snapshots de determine if file is still being written
		long fileSize1 = client.getSize(fileName);
		long fileSize2 = client.getSize(fileName);
		
		if( fileSize1 == fileSize2 )
		{
			logger.debug("File is stable (not growing), ready for retrieval: " + fileName);

			return false;
		}
		else
		{
			logger.debug("File is growing, deferring retrieval: " + fileName); 
			return true;
		}    	
    }
    
}