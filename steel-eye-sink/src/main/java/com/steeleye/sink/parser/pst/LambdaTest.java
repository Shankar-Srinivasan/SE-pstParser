package com.steeleye.sink.parser.pst;

import java.io.BufferedReader;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

import java.util.ArrayList;
import java.util.List;
import java.util.Vector;

import org.apache.log4j.Logger;

import com.amazonaws.services.lambda.runtime.Context;
import com.amazonaws.services.lambda.runtime.LambdaLogger;
import com.amazonaws.services.lambda.runtime.RequestStreamHandler;
import com.amazonaws.services.lambda.runtime.events.SNSEvent;
import com.amazonaws.services.lambda.runtime.events.SNSEvent.SNS;
import com.amazonaws.services.s3.AmazonS3;
import com.amazonaws.services.s3.AmazonS3ClientBuilder;
import com.amazonaws.services.s3.event.S3EventNotification;
import com.amazonaws.services.s3.event.S3EventNotification.S3EventNotificationRecord;
import com.fasterxml.jackson.databind.MapperFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.joda.JodaModule;
import com.pff.PSTException;
import com.pff.PSTFile;
import com.pff.PSTFolder;
import com.pff.PSTMessage;
import com.steeleye.sink.parser.EMail; 

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
	static
	{
		//following lines added to deal with the issues in  Serialization and deserialization of SNSEvent
		jsonMapper.configure(MapperFeature.ACCEPT_CASE_INSENSITIVE_PROPERTIES, true);
		jsonMapper.registerModule(new JodaModule());
	}
	static final Logger log4jLogger = Logger.getLogger(LambdaTest.class);
	private LambdaLogger logger;
/**
 * Simple PST to JSON tests
 * 
 * @param args args[0] = fully qualified pst file name. The json files are stored in the same folder.
 */
	public static void main(String[] args) 
	{
		
		//get pst from a file stored in the local file system. File name is input as the first parameter to the program
		//it is broken into multiple json files, one each for every mail within the pst, and the json files are stored in the same folder as the pst. 
		/*
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
					
					AmazonS3 s3 = AmazonS3ClientBuilder.defaultClient();
					jsonFileName = eMail.getDescriptorNodeId()+"-"+eMail.getDeliveryDttm();
					s3.putObject("steel-eye-test-json-mail",jsonFileName,jsonMapper.writeValueAsString(eMail));
					
				}
			}
			
		}
		catch(Exception e)
		{
			e.printStackTrace();
		}
		*/
		
		/*
		//reads the JSON sent by SNS to the lambda when a file is copied into the S3 folder 
		try
		{
			BufferedReader br = new BufferedReader(new FileReader(args[0]));
	
			StringBuilder fileContents = new StringBuilder(); 
			String nextLine = br.readLine();
			while(nextLine !=null) 
			{
				fileContents.append(nextLine);
				nextLine = br.readLine();
			}
			br.close();
			SNSEvent snsEvent = jsonMapper.readValue(fileContents.toString(),SNSEvent.class );
			SNSEvent.SNSRecord out = snsEvent.getRecords().get(0);
			System.out.println(out.getSNS().getMessage());
			S3EventNotification s3EventNotification = S3EventNotification.parseJson(out.getSNS().getMessage());
			if(s3EventNotification!=null && s3EventNotification.getRecords()!=null)
			{	
				for (S3EventNotificationRecord notificationRecord : s3EventNotification.getRecords()) 
				{
					String s3Key = notificationRecord.getS3().getObject().getKey();
					String s3Bucket = notificationRecord.getS3().getBucket().getName();
					System.out.println("s3Key:"+s3Key+" s3Bucket:"+s3Bucket+"\n" );	
					
				}
			}
		}
		catch(Exception e)
		{
			e.printStackTrace();
		}
		*/
		
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
		AmazonS3 s3 = AmazonS3ClientBuilder.defaultClient();
		StringBuilder tempDebugLog = new StringBuilder();
		SNSEvent snsEvent = jsonMapper.readValue(inputStream, SNSEvent.class);
		tempDebugLog.append("Parsed the SNSEvent\n");
		
		for (SNSEvent.SNSRecord snsRecord : snsEvent.getRecords()) 
		{
			SNS sns = snsRecord.getSNS();
			tempDebugLog.append("Got the SNS\n");
			S3EventNotification s3EventNotification = S3EventNotification.parseJson(sns.getMessage());
			
			tempDebugLog.append("Got the SNS Notification\n");
			for (S3EventNotificationRecord notificationRecord : s3EventNotification.getRecords()) 
			{
				tempDebugLog.append("Got the Notification record\n");
				String s3Key = notificationRecord.getS3().getObject().getKey();
			    String s3Bucket = notificationRecord.getS3().getBucket().getName();
			    tempDebugLog.append("Got the key:"+s3Key+" and bucket:"+s3Bucket +"\n");
			    
			    InputStream pstFileInputStream = s3.getObject(s3Bucket,s3Key).getObjectContent();
			    
			    byte [] fileContent = null;
				if(pstFileInputStream!=null)
				{
					ByteArrayOutputStream buffer = new ByteArrayOutputStream();
					int nRead;
					byte[] data = new byte[16384];
	
					while ((nRead = pstFileInputStream.read(data, 0, data.length)) != -1) 
					{
					  buffer.write(data, 0, nRead);
					}
					buffer.flush();
					fileContent = buffer.toByteArray();
					pstFileInputStream.close();
				}
				
				if(fileContent!=null)
				{
					try
					{
						PSTFile pstFile = new PSTFile(fileContent);
						List<EMail> listOfEMails = getEMailsFromPst(pstFile);
						if(listOfEMails!=null)
						{
							tempDebugLog.append("Got "+listOfEMails.size() +" mails\n");
							for(EMail eMail: listOfEMails)
							{
								String jsonFileName = eMail.getDescriptorNodeId()+"-"+eMail.getDeliveryDttm();
								s3.putObject("steel-eye-test-json-mail",jsonFileName,jsonMapper.writeValueAsString(eMail));
							}
						}
					}
					catch (PSTException e)
					{
						e.printStackTrace();
						tempDebugLog.append(e);
					}
				}
			}
		}
		
		//debug log
		log(tempDebugLog.toString());
		s3.putObject("steel-eye-code","DebugLog"+System.currentTimeMillis(),tempDebugLog.toString());
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
	protected void log (String logString)
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