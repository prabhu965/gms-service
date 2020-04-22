package org.codealpha.gmsservice.services;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;

import javax.mail.MessagingException;
import javax.mail.internet.MimeMessage;
import java.io.UnsupportedEncodingException;

@Component
@ComponentScan
public class CommonEmailSevice {

  @Autowired
  private JavaMailSender mailSender;

  @Value("${spring.send-mail}")
  private boolean sendMail;

  @Async("threadPoolTaskExecutor")
  public void sendMail(String to,String[] ccList, String subject, String messageText, String footer[]){
    if(!sendMail){
      return;
    }

    try {
      MimeMessage message = mailSender.createMimeMessage();
      MimeMessageHelper mimeMessageHelper = new MimeMessageHelper(message);
      //SimpleMailMessage message = new SimpleMailMessage();
      mimeMessageHelper.setTo(to);
      if(ccList!=null) {
        mimeMessageHelper.setCc(ccList);
      }
      mimeMessageHelper.setFrom("admin@anudan.org","Anudan Admin");
      mimeMessageHelper.setSubject(subject);
      for(String footerBlock: footer){
          messageText = messageText.concat(footerBlock);
      }
      mimeMessageHelper.setText(messageText,true);
      mailSender.send(message);
    }catch (MessagingException | UnsupportedEncodingException mse){
      mse.printStackTrace();
    }
  }

}
