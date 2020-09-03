package com.company;
import javax.swing.*;
import java.awt.*;

public class MainActivity {

    JFrame jf=new JFrame();
    JProgressBar bar=new JProgressBar();
    JLabel jl;
    int counter=0;

    public MainActivity() {
        bar.setValue(counter);
        bar.setBounds(0,400,500,50);
        bar.setForeground(Color.RED);
        bar.setBackground(Color.BLACK);
        bar.setStringPainted(true);
        jf.add(bar);

        jl=new JLabel();
        jl.setHorizontalTextPosition(SwingConstants.CENTER);
        jl.setHorizontalAlignment(SwingConstants.CENTER);
        jl.setBounds(0,300,150,25);
        jf.add(jl);

        jf.setSize(500,500);
        jf.setLayout(null);
        jf.setLocationRelativeTo(null);
        jf.setVisible(true);

        fill();
    }
    public void fill(){
        int i=0;
            for(int counter=0; counter<=100; counter++) {
                bar.setValue(counter);
                i++;
                try {
                    Thread.sleep(10);
                    if (i == 10) {
                        jl.setText("guru");
                    }
                    if (i == 50){
                        jl.setText("That good guru");
                    }
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
            jf.dispose();
            new LogIn();
    }

    public static void main(String[] args) {
        MainActivity main=new MainActivity();
    }
}
