package com.steeleye.sink.parser;

import java.util.Date;

import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.annotation.JsonPOJOBuilder;
import com.pff.PSTMessage;

/**
 * Simple immutable POJO for eMails
 * @author Shankar
 *
 */
@JsonDeserialize(builder = EMail.Builder.class)
public class EMail 
{
	private final String subject;
	private final String senderMailId;
	private final String toMailIds;
	private final String body; 
	private final Date deliveryDttm;
	private final boolean hasAttachment;
	private final long descriptorNodeId;

	private EMail(Builder builder) 
	{
		subject 			= builder.subject;
		senderMailId		= builder.senderMailId;
		toMailIds			= builder.toMailIds;
		body				= builder.body; 
		deliveryDttm		= builder.deliveryDttm;
		hasAttachment		= builder.hasAttachment;
		descriptorNodeId	= builder.descriptorNodeId;	
	}
	
	public String getSubject() 
	{
		return subject;
	}

	public String getSenderMailId() 
	{
		return senderMailId;
	}
	
	public String getToMailIds() 
	{
		return toMailIds;
	}
	
		public String getBody() 
	{
		return body;
	}
	
	public boolean getHasAttachment() 
	{
		return hasAttachment;
	}
	
	public Date getDeliveryDttm() 
	{
		return deliveryDttm;
	}

	public long getDescriptorNodeId() 
	{
		return descriptorNodeId;
	}
	
	@Override
	public int hashCode()
	{
		int hashCode = (int) descriptorNodeId >>> 32;
		if(deliveryDttm!=null)
			hashCode = (31*hashCode) + deliveryDttm.hashCode();
		return hashCode;
	}
	
	@Override
	public String toString()
	{
		return "Email"											+ "\n"	+
			   "From:"				+ this.getSenderMailId()	+ "\n"	+
			   "To:"				+ this.getToMailIds()		+ "\n"	+
			   "Delivered At:"		+ this.getDeliveryDttm()  	+ "\n"	+
			   "has attachment?:"	+ this.hasAttachment;
	}
	
	@Override 
	public boolean equals(Object anotherEMail)
	{
		boolean result = false;
		if(anotherEMail!=null)
		{
			if(anotherEMail==this)
			{
				result = true;
			}	
			else if(anotherEMail instanceof EMail)
			{
				EMail theOtherEMail = (EMail)anotherEMail;
				
				if(descriptorNodeId==theOtherEMail.getDescriptorNodeId() &&
					(senderMailId!=null && senderMailId.equals(theOtherEMail.getSenderMailId()))  &&
					(toMailIds!=null && toMailIds.equals(theOtherEMail.getToMailIds()))  &&
					(body!=null && body.equals(theOtherEMail.getBody()))  )
				{
					result = true;
				}
			}
		}
		return result;
		
	}
	
	/**
	 * Static Builder class for EMail 
	 * @author Shankar
	 *
	 */
	@JsonPOJOBuilder(withPrefix="set")
	public static class Builder
	{
		private String subject;
		private String senderMailId;
		private String toMailIds;
		private String body; 
		private Date deliveryDttm;
		private boolean hasAttachment;
		private long descriptorNodeId;
		
		public Builder()
		{
			
		}
		
		public Builder(PSTMessage pstMsg)
		{
			subject				= 	pstMsg.getSubject();
			senderMailId		=	pstMsg.getSenderEmailAddress();
			toMailIds			=	pstMsg.getDisplayTo();
			body				=	pstMsg.getBody();
			deliveryDttm		=	pstMsg.getMessageDeliveryTime();
			hasAttachment		=	pstMsg.hasAttachments();
			descriptorNodeId	=	pstMsg.getDescriptorNodeId();
		}
		
		public String getSubject() 
		{
			return subject;
		}

		public String getSenderMailId() 
		{
			return senderMailId;
		}

		public String getToMailIds() 
		{
			return toMailIds;
		}

		public String getBody() 
		{
			return body;
		}

		public Date getDeliveryDttm() 
		{
			return deliveryDttm;
		}

		public boolean isHasAttachment() 
		{
			return hasAttachment;
		}

		public long getDescriptorNodeId() 
		{
			return descriptorNodeId;
		}

		public void setSubject(String subject) 
		{
			this.subject = subject;
		}

		public void setSenderMailId(String senderMailId) 
		{
			this.senderMailId = senderMailId;
		}

		public void setToMailIds(String toMailIds) 
		{
			this.toMailIds = toMailIds;
		}

		public void setBody(String body) 
		{
			this.body = body;
		}

		public void setDeliveryDttm(Date deliveryDttm) 
		{
			this.deliveryDttm = deliveryDttm;
		}

		public void setHasAttachment(boolean hasAttachment) 
		{
			this.hasAttachment = hasAttachment;
		}

		public void setDescriptorNodeId(long descriptorNodeId) 
		{
			this.descriptorNodeId = descriptorNodeId;
		}

		public EMail build()
		{
			if (	(subject==null)									||
					(senderMailId==null || senderMailId.isEmpty()) 	||
					(toMailIds==null 	|| toMailIds.isEmpty()) 	||
					(senderMailId==null || senderMailId.isEmpty()) 	||
					(deliveryDttm==null)
				)
				{
					throw new IllegalArgumentException();
				}
			
			return new EMail(this);
		}
		
		@Override
		public int hashCode()
		{
			int hashCode = (int) descriptorNodeId >>> 32;
			//will need to update for non MS mails because they wouldn't have descriptor Node Id
			return hashCode;
		}
		
		@Override
		public String toString()
		{
			return "Email"											+ "\n"	+
				   "From:"				+ this.getSenderMailId()	+ "\n"	+
				   "To:"				+ this.getToMailIds()		+ "\n"	+
				   "Delivered At:"		+ this.getDeliveryDttm()  	+ "\n"	+
				   "has attachment?:"	+ this.hasAttachment;
		}
	}
	
}
