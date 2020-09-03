package com.company;

import javax.swing.*;
import java.awt.*;

public class LogIn extends JFrame {

    JFrame jf=new JFrame();
    JTextField jt1;
    JPasswordField pf1;
    JButton jb1;

    LogIn()  {
        jf.setSize(500,500);
        jf.setLayout(null);
        jf.setLocationRelativeTo(null);
        jf.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);

        jt1=new JTextField();
        jt1.setBounds(180,60,100,35);
        jf.add(jt1);

        pf1= new JPasswordField();
        pf1.setBounds(180,120,100,35);
        jf.add(pf1);

        jb1=new JButton();
        jb1.setBounds(190,160,75,35);
        jb1.setText("Login");
        jf.add(jb1);


        jf.setVisible(true);
    }
}
