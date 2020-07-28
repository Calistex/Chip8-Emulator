package emu;

import chip.Chip;

import javax.swing.*;
import java.awt.*;

public class ChipPanel extends JPanel {

    private Chip chip;
    private int scale;

    public ChipPanel(Chip chip) {
        this.chip = chip;
        this.scale = 10;
    }

    public void paint(Graphics g){
        byte[] display = chip.getDisplay();
        for(int i = 0; i < display.length; i++){
            if(display[i] == 0){
                g.setColor(Color.BLACK);
            } else {
                g.setColor(Color.WHITE);
            }

            if(chip.isInSuperMode()){
                int x = (i % 128);
                int y = (int)Math.floor(i / 128);

                g.fillRect(x * scale/2, y * scale/2, scale/2, scale/2);
            } else {
                int x = (i % 64);
                int y = (int)Math.floor(i / 64);

                g.fillRect(x * scale, y * scale, scale, scale );
            }
        }
    }

    public void setScale(int x){
        this.scale = x;
    }
}
