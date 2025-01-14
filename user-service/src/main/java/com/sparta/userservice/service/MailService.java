package com.sparta.userservice.service;

import com.sparta.common.exception.DuplicateEmailException;
import com.sparta.common.exception.MailSendFailureException;
import com.sparta.userservice.repository.UserRepository;
import jakarta.mail.MessagingException;
import jakarta.mail.internet.MimeMessage;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.MailException;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Service;

import java.util.Random;

@Service
@RequiredArgsConstructor
public class MailService {

    private final JavaMailSender javaMailSender;
    private final UserRepository userRepository;

    @Value("${spring.mail.username}") // 발신주소 설정
    private String senderEmail;

    // 랜덤 인증번호 생성
    public String createNumber() {
        Random random = new Random();
        StringBuilder key = new StringBuilder();

        for (int i = 0; i < 6; i++) { // 6자리 인정번호
            int index = random.nextInt(3); //  0-2 사이의 랜덤값으로 스위치문 실행

            switch (index) {
                case 0 -> key.append((char) (random.nextInt(26) + 97)); // 소문자
                case 1 -> key.append((char) (random.nextInt(26) + 65)); // 대문자
                case 2 -> key.append(random.nextInt(10)); // 숫자
            }
        }
        System.out.println("생성된 인증번호 : " + key.toString());
        return key.toString();
    }

    // 메일 본문 만들기
    public MimeMessage createMail(String mail, String number) throws MessagingException {
        MimeMessage message = javaMailSender.createMimeMessage();
        message.setFrom(senderEmail);
        message.setRecipients(MimeMessage.RecipientType.TO, mail);
        message.setSubject("이메일 인증"); // 제목
        String body = "";
        body += "<h3>요청하신 인증 번호입니다.</h3>";
        body += "<h1>" + number + "</h1>";
        body += "<h3>감사합니다.</h3>";
        message.setText(body, "UTF-8", "html");
        return message;
    }

    // 메일 전송
    public String sendMail(String sendEmail) throws MessagingException {
        if (userRepository.existsByEmail(sendEmail)) {
            throw new DuplicateEmailException("이미 가입된 이메일입니다.");
        }
        System.out.println("이메일 중복체크 완료");

        String number = createNumber();
        MimeMessage message = createMail(sendEmail, number);
        try {
            javaMailSender.send(message);
        } catch (MailException e) {
            throw new MailSendFailureException("이메일 인증번호 발송 중 오류가 발생했습니다.", e);
        }
        System.out.println("인증번호 전송 완료. 본문내용: " + message);
        return number;
    }

}
