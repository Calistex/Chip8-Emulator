package emu;

import chip.Chip;

import javax.sound.sampled.*;
import java.io.File;

public class Main extends Thread {

    private Chip chip8;
    private ChipFrame frame;

    public Main() {
        chip8 = new Chip();
        chip8.init();
//        String filename = "tetris";
//        chip8.loadProgram("./" + filename);
        frame = new ChipFrame(chip8);
        File file = frame.openFile();
        chip8.loadProgram(file);
    }

    public void run() {
        while (true) {
            if (!frame.isEmulationPaused()) {
                chip8.setKeyBuffer(frame.getKeyBuffer());
                chip8.run();
                if (chip8.needsSound()) {
                    //Qui dovrebbe fare BEEP
                    System.out.println("BEEP!");
                    if (frame.isAudioEnabled()) {
                        playSound();
                    }
                    chip8.removeSoundFlag();
                }
                if (chip8.needsRedraw()) {
                    frame.repaint();
                    chip8.removeDrawFlag();
                }
                try {
                    Thread.sleep(4);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            } else {
                System.out.println("PAUSED");
            }
        }
    }

    public static void main(String[] args) {
        Main main = new Main();
        main.start();
    }

    public Chip getChip8() {
        return chip8;
    }

    public ChipFrame getFrame() {
        return frame;
    }

    public void playSound() {
        new Thread("Audio thread") {
            public void run() {
                try {
                    File audioFile = new File("./beep.wav");
                    AudioInputStream stream;
                    AudioFormat format;
                    DataLine.Info info;
                    Clip clip;

                    stream = AudioSystem.getAudioInputStream(audioFile);
                    format = stream.getFormat();
                    info = new DataLine.Info(Clip.class, format);
                    clip = (Clip) AudioSystem.getLine(info);
                    clip.open(stream);
                    clip.start();
                } catch (Exception e) {
                    //whatevers
                }
            }
        }.start();
    }
}
