package com.steeleye.sink.parser.pst;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.Vector;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.pff.*;
import com.steeleye.sink.parser.EMail;

/**
 * Simple Test to convert pst files into json
 * 
 * @author Shankar
 *
 */
public class LambdaTest 
{

	private int depth = -1;
	private static ObjectMapper jsonMapper = new ObjectMapper();
	private String pstFilename;
	
	public static void main(String[] args) 
	{
		try
		{
			new LambdaTest().parseAndLogPstFile(args[0]);
		}
		catch(Exception e)
		{
			e.printStackTrace();
		}
	}

	/**
	 * Breaks down the input pstfile into eMails in Json .
	 * 
	 * @param fileName : Path + name of a valid pst file 
	 * @throws FileNotFoundException if the file is not found
	 * @throws PSTException
	 * @throws IOException
	 */
	public void parseAndLogPstFile(String fileName) throws FileNotFoundException, PSTException, IOException 
	{
		this.pstFilename=fileName;
		PSTFile pstFile = new PSTFile(fileName);
		System.out.println(pstFile.getMessageStore().getDisplayName());
        processFolder(pstFile.getRootFolder());
    }

	/**
	 * Breaks down the input PSTFolder object into Emails in Json format
	 * 
	 * @param folder The PSTFolder to be processed
	 * @throws PSTException
	 * @throws IOException
	 */
	protected void processFolder(PSTFolder folder) throws PSTException, IOException
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
	            processFolder(childFolder);
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
	            String jsonFileName = pstFilename+"-"+eMail.getDescriptorNodeId()+"-"+eMail.getDeliveryDttm();
	            jsonMapper.writeValue(new File(jsonFileName), eMail);
	            pstMessage = (PSTMessage)folder.getNextChild();
	        }
	        depth--;
		}
		depth--;
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