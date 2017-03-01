package com.steeleye.sink.parser.pst;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.Vector;
import org.apache.log4j.Logger;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.pff.*;
import com.steeleye.sink.parser.EMail;
import com.amazonaws.services.lambda.runtime.Context;
import com.amazonaws.services.lambda.runtime.LambdaLogger;
import com.amazonaws.services.lambda.runtime.RequestStreamHandler;
import com.amazonaws.services.s3.AmazonS3;
import com.amazonaws.services.s3.AmazonS3ClientBuilder; 

/**
 * Convert pst files into emails in json
 * 
 * @author Shankar
 *
 */
public class LambdaTest implements RequestStreamHandler 
{

	private int depth = -1;
	private static ObjectMapper jsonMapper = new ObjectMapper();
	static final Logger log4jLogger = Logger.getLogger(LambdaTest.class);
	private LambdaLogger logger;
/**
 * Simple Test that splits a pst file in the file system into component emails in json format
 * 
 * @param args args[0] = fully qualified pst file name. The json files are stored in the same folder.
 */
	public static void main(String[] args) 
	{
		try
		{
			List<EMail> listOfEMails = new LambdaTest().getEMailsFromPst(args[0]);
			if(listOfEMails!=null)
			{
				for(EMail eMail: listOfEMails)
				{
					String jsonFileName = args[0]+"-"+eMail.getDescriptorNodeId()+"-"+eMail.getDeliveryDttm();
					jsonMapper.writeValue(new File(jsonFileName), eMail);
					
					//test if the messages get written into the json bucket
					/*
					AmazonS3 s3 = AmazonS3ClientBuilder.defaultClient();
					jsonFileName = eMail.getDescriptorNodeId()+"-"+eMail.getDeliveryDttm();
					s3.putObject("steel-eye-test-json-mail",jsonFileName,jsonMapper.writeValueAsString(eMail));
					*/
					
				}
			}
			
		}
		catch(Exception e)
		{
			e.printStackTrace();
		}
	}
	
	/**
	 * Amazon Lambda method invoked when a pst file is placed in the steel-eye-test-pst bucket
	 * It splits the pst file into multiple emails in json format, and places them in the steel-eye-test-json-mail bucket
	 * 
	 * @param inputStream
	 * @throws IOException
	 */
	public void handleRequest(InputStream inputStream, OutputStream outputStream, Context context)
	        throws IOException 
	{
		
		logger = context.getLogger();
		log("Entering handleRequest.");

		
		byte [] fileContent = null;
		List<EMail> listOfEMails  = null;
		if(inputStream!=null)
		{
			log("Have got a non-null inputStream.");
			
			ByteArrayOutputStream buffer = new ByteArrayOutputStream();
			int nRead;
			byte[] data = new byte[16384];

			while ((nRead = inputStream.read(data, 0, data.length)) != -1) 
			{
			  buffer.write(data, 0, nRead);
			}
			buffer.flush();
			fileContent = buffer.toByteArray();
		}

		log("Attempting to process the pst file content");
		if(fileContent!=null)
		{
			log("The pst is not null");
			try 
			{
				PSTFile pstFile = new PSTFile(fileContent);
				listOfEMails = getEMailsFromPst(pstFile);
			} 
			catch (PSTException e) 
			{
			
				e.printStackTrace();
				logger.log(e.toString());
			}
		}
		
		log("Attempting to write the eMails");
		
		if(listOfEMails!=null)
		{
			AmazonS3 s3 = AmazonS3ClientBuilder.defaultClient();
			for(EMail eMail: listOfEMails)
			{
				String jsonFileName = eMail.getDescriptorNodeId()+"-"+eMail.getDeliveryDttm();
				s3.putObject("steel-eye-test-json-mail",jsonFileName,jsonMapper.writeValueAsString(eMail));
				log("Wrote "+jsonFileName+" into the bucket:");
			}
		}
	}

	/**
	 * 
	 * @param string fileName
	 * @return List<EMail> 
	 * @throws IOException 
	 * @throws PSTException 
	 * @throws FileNotFoundException 
	 */
	
	public List<EMail> getEMailsFromPst(String fileName) throws FileNotFoundException, PSTException, IOException 
	{
		PSTFile pstFile = new PSTFile(fileName);
		return getEMailsFromPst(pstFile);
	}


	/**
	 * 
	 * @param pstFile
	 * @return List<EMail>
	 * @throws IOException 
	 * @throws PSTException 
	 */
	public List<EMail> getEMailsFromPst(PSTFile pstFile) throws PSTException, IOException 
	{
		if(pstFile==null)
		{
			return new ArrayList<EMail>();
		}
		System.out.println(pstFile.getMessageStore().getDisplayName());
		return getEMailsInPstFolder(pstFile.getRootFolder());
		
	}


	/**
	 * returns a list of EMail within this PSTFolder and it's subfolders 
	 * 
	 * @param folder The PSTFolder to be processed
	 * @throws PSTException
	 * @throws IOException
	 */
	protected List<EMail> getEMailsInPstFolder(PSTFolder folder) throws PSTException, IOException
	{
		List <EMail> listOfMails = new ArrayList<EMail>();
		
		if(folder!=null)
		{
			depth++;
			// the root folder doesn't have a display name
			if (depth > 0) 
			{
				printDepth();
				System.out.println(folder.getDisplayName());
			}
			// go through the subfolders...
			if (folder.hasSubfolders()) 
			{
		        Vector<PSTFolder> childFolders = folder.getSubFolders();
		        for (PSTFolder childFolder : childFolders) 
		        {
		        	listOfMails.addAll(getEMailsInPstFolder(childFolder));
		        }
			}
	
			// and now the emails for this folder
			if (folder.getContentCount() > 0) 
			{
		        depth++;
		        PSTMessage pstMessage = (PSTMessage)folder.getNextChild();
		        while (pstMessage != null) 
		        {
		            printDepth();
		            EMail eMail = new EMail.Builder(pstMessage).build();
		            System.out.println(eMail);
		            listOfMails.add(eMail);
		            pstMessage = (PSTMessage)folder.getNextChild();
		        }
		        depth--;
			}
			depth--;
		}
		return listOfMails;
	}

	/**
	 * make appropriate log entries
	 * @param logString
	 */
	private void log (String logString)
	{
		System.out.println(logString);
		if(logger!=null)
			logger.log("LambdaLogger"+logString+"\n");
		
		if(log4jLogger!=null)
		{
			log4jLogger.debug("Log4j Debug"+logString+"\n");
			log4jLogger.error("Log4j Error"+logString+"\n");
		}
		
	}
	
	protected void printDepth() 
	{
	    for (int x = 0; x < depth-1; x++) 
	    {
	        System.out.print(" | ");
	    }
	    System.out.print(" |- ");
	}
}