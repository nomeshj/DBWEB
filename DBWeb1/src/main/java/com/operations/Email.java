package com.operations;

import javax.mail.*;
import javax.mail.internet.*;

import java.io.File;
import java.io.FileInputStream;
import java.util.Enumeration;

//import com.example.Main;

import java.util.Properties;

public class Email {
 
    private String emailAddressTo = new String();
    private String msgSubject = new String();
    private String msgText = new String();

    final String USER_NAME = "";   //User name of the Goole(gmail) account
    final String PASSSWORD = "";  //App Password of the Goole(gmail) account
    
    private String Content;
    private String Recipeints;
    private String emailSubject;
    public boolean status =false;
   
    public Email() {
    }
    public Email(String Content,String Recipeints,String emailSubject) {
    	this.Content=Content;
    	this.Recipeints = Recipeints;
    	this.emailSubject = emailSubject;
    }
    
    public void setEmailAddressTo(String emailAddressTo) {
        this.emailAddressTo = emailAddressTo;
    }

    public void setSubject(String subject) {
        this.msgSubject = subject;
    }

    public void setMessageText(String msgText) {
        this.msgText = msgText;
    }

    public boolean createAndSendEmail(String emailAddressTo, String msgSubject, String msgText) {
    	this.emailAddressTo = emailAddressTo;
    	this.msgSubject = msgSubject;
    	this.msgText = msgText;
    	return sendEmailMessage();
    }
    
    private boolean sendEmailMessage() {
        
        //Create email sending properties
        Properties props = new Properties();
        props.put("mail.smtp.auth", "true");
        props.put("mail.smtp.starttls.enable", "true");
        props.put("mail.smtp.host", "smtp.gmail.com");
        props.put("mail.smtp.port", "587");
        props.put("mail.smtp.ssl.protocols", "TLSv1.2");
     
       Session session = Session.getInstance(props,
       new javax.mail.Authenticator() {
       protected PasswordAuthentication getPasswordAuthentication() {
       return new PasswordAuthentication(USER_NAME, PASSSWORD);
      }
       });

     try {

        Message message = new MimeMessage(session);
        message.setFrom(new InternetAddress(USER_NAME)); //Set from address of the email
        message.setContent(msgText,"text/html"); //set content type of the email
       message.setRecipients(Message.RecipientType.TO,InternetAddress.parse(Recipeints)); //Set email recipient
       
       message.setSubject(emailSubject); //Set email message subject
       Transport.send(message); //Send email message
       
       status =true;
      System.out.println("sent email successfully!");

     } catch (MessagingException e) {
    	 status =false;
          throw new RuntimeException(e);
     }
     return status;
       }
    
    public boolean send() throws Exception{
     String cnt = Content;
     Recipeints = Recipeints.replace(";", ",");
      return createAndSendEmail(USER_NAME, emailSubject,cnt);
    }

}
