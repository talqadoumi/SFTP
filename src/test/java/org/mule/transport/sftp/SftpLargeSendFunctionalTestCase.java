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

import java.io.FileInputStream;
import java.util.HashMap;

import org.mule.module.client.MuleClient;
import org.mule.tck.FunctionalTestCase;
import org.mule.transport.sftp.SftpConnector;

/**
 * <code>LargeFileSendFunctionalTestCase</code> tests sending a large file message
 * from an sftp service.  
 * 
 * 
 */

public class SftpLargeSendFunctionalTestCase extends FunctionalTestCase
{

	protected static final String FILEPATH = "/tmp/";
	protected static final String FILENAME = "big.zip";
    
    protected String getConfigResources()
    {
        return "mule-large-send-test-config.xml";
    }

    
    //Place a large file in the directory specified by FILEPATH + FILENAME 
 
    public void testSendLargeFile() throws Exception
    {
    	
        MuleClient client = new MuleClient();  
        
        HashMap props = new HashMap(1);
        props.put(SftpConnector.PROPERTY_FILENAME,FILENAME);
        
        FileInputStream data = new FileInputStream(FILEPATH + FILENAME);
        
        client.send("vm://test.upload",data,props);  	


    }


}