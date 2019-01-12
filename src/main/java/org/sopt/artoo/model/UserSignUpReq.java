package org.sopt.artoo.model;

import lombok.Data;

@Data
public class UserSignUpReq {
    //회원 가입시 입력

    private String u_email;
    // 유저 ID(이메일)
    private String u_pw;
    // 유저 비밀번호
    private String u_school;
    // 유저 학교
    private String u_phone;
    // 유저 핸드폰 번호
    private String u_address;
    // 유저 주소
    private String u_name;
    // 유저 이름
    private String u_bank ="";
    // 은행명
    private String u_account;
    // 유저 계좌 번호
    private String u_dept;
    //유저 전공
    private String u_description;
    // 유저 정보 변경

    public boolean checkQualification() {
        return (u_email != null && u_pw != null && u_phone != null && u_name != null);
    }
}
