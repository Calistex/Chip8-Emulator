package emu;

import chip.Chip;

import javax.swing.*;
import java.awt.*;
import java.awt.event.*;
import java.io.File;

public class ChipFrame extends JFrame implements KeyListener {

    private ChipPanel panel;
    private int[] keyBuffer;
    private int[] keyIdToKey;
    private File gameFile;
    private boolean audioEnabled;
    private boolean emulationPaused;
    private boolean resetGame;

    public ChipFrame(Chip chip) {
        setPreferredSize(new Dimension(620, 320));
        pack();
        setPreferredSize(new Dimension(640 + getInsets().left + getInsets().right,
                343 + getInsets().top + getInsets().bottom));

        panel = new ChipPanel(chip);
        setLayout(new BorderLayout());
        add(panel, BorderLayout.CENTER);

        JMenuBar menuBar = new JMenuBar();
        JMenu menu = new JMenu("Menu");
        menu.setMnemonic(KeyEvent.VK_A);
        menuBar.add(menu);

        JMenuItem audioMenuItem = new JCheckBoxMenuItem("Audio Enabled");
        audioEnabled = true;
        audioMenuItem.setSelected(true);
        audioMenuItem.addActionListener(
                (event) -> {
                    audioMenuItem.setSelected(!audioEnabled);
                    audioEnabled = !audioEnabled;
                }
        );
        audioMenuItem.setMnemonic(KeyEvent.VK_M);
        audioMenuItem.setAccelerator(KeyStroke.getKeyStroke(
                KeyEvent.VK_M, InputEvent.ALT_MASK));
        menu.add(audioMenuItem);

        menu.addSeparator();

        JMenuItem pauseMenuItem = new JMenuItem("Pause/Resume",
                KeyEvent.VK_P);
        pauseMenuItem.addActionListener(
                (event) -> emulationPaused = !emulationPaused
        );
        pauseMenuItem.setMnemonic(KeyEvent.VK_P);
        pauseMenuItem.setAccelerator(KeyStroke.getKeyStroke(
                KeyEvent.VK_P, InputEvent.ALT_MASK));
        menu.add(pauseMenuItem);

        JMenuItem resetMenuItem = new JMenuItem("Reset",
                KeyEvent.VK_T);
        resetMenuItem.addActionListener(
                (event) -> resetGame = true
        );
        resetMenuItem.setMnemonic(KeyEvent.VK_T);
        resetMenuItem.setAccelerator(KeyStroke.getKeyStroke(
                KeyEvent.VK_T, InputEvent.ALT_MASK));
        menu.add(resetMenuItem);

        JMenuItem exitMenuItem = new JMenuItem("Exit",
                KeyEvent.VK_T);
        exitMenuItem.addActionListener(
                (event) -> System.exit(0)
        );
        menu.add(exitMenuItem);

        setJMenuBar(menuBar);

        setDefaultCloseOperation(WindowConstants.EXIT_ON_CLOSE);
        setWindowTitle(null);
        pack();
        setVisible(true);
        addKeyListener(this);

        keyIdToKey = new int[256];
        keyBuffer = new int[16];
        fillKeyIds();
    }

    public ChipFrame(Chip chip, String gameTitle) {
        setPreferredSize(new Dimension(640, 320));
        pack();
        setPreferredSize(new Dimension(640 + getInsets().left + getInsets().right,
                320 + getInsets().top + getInsets().bottom));

        panel = new ChipPanel(chip);
        setLayout(new BorderLayout());
        add(panel, BorderLayout.CENTER);
        setDefaultCloseOperation(WindowConstants.EXIT_ON_CLOSE);
        setWindowTitle(gameTitle);
        pack();
        setVisible(true);
        addKeyListener(this);

        keyIdToKey = new int[256];
        keyBuffer = new int[16];
        fillKeyIds();
    }

    public void setWindowTitle(String gameTitle) {
        if (gameTitle == null || gameTitle.isEmpty()) {
            setTitle("Chip 8 Emulator");
        } else {
            setTitle("Chip 8 Emulator - " + gameTitle);
        }
    }

    private void fillKeyIds() {
        for (int i = 0; i < keyIdToKey.length; i++) {
            keyIdToKey[i] = -1;
        }
        keyIdToKey['1'] = 1;
        keyIdToKey['2'] = 2;
        keyIdToKey['3'] = 3;
        keyIdToKey['Q'] = 4;
        keyIdToKey['W'] = 5;
        keyIdToKey['E'] = 6;
        keyIdToKey['A'] = 7;
        keyIdToKey['S'] = 8;
        keyIdToKey['D'] = 9;
        keyIdToKey['Z'] = 0xA;
        keyIdToKey['X'] = 0;
        keyIdToKey['C'] = 0xB;
        keyIdToKey['4'] = 0xC;
        keyIdToKey['R'] = 0xD;
        keyIdToKey['F'] = 0xE;
        keyIdToKey['V'] = 0xF;
    }

    @Override
    public void keyPressed(KeyEvent e) {
        if (keyIdToKey[e.getKeyCode()] != -1) {
            keyBuffer[keyIdToKey[e.getKeyCode()]] = 1;
        }
    }

    @Override
    public void keyReleased(KeyEvent e) {
        if (keyIdToKey[e.getKeyCode()] != -1) {
            keyBuffer[keyIdToKey[e.getKeyCode()]] = 0;
        }
    }

    @Override
    public void keyTyped(KeyEvent e) {
    }

    public int[] getKeyBuffer() {
        return keyBuffer;
    }

    public File getGameFile() {
        return gameFile;
    }

    public File openFile() {
        JFileChooser fileChooser = new JFileChooser();
        fileChooser.setCurrentDirectory(new java.io.File("."));
        int result = fileChooser.showOpenDialog(this);
        if (result == JFileChooser.APPROVE_OPTION) {
            File selectedFile = fileChooser.getSelectedFile();
            System.out.println("Selected file: " + selectedFile.getAbsolutePath());
            setWindowTitle(selectedFile.getName());
            gameFile = selectedFile;
            return selectedFile;
        } else {
            return null;
        }
    }

    public boolean isAudioEnabled() {
        return audioEnabled;
    }

    public boolean isEmulationPaused() {
        return emulationPaused;
    }

    public boolean resetGame(){
        return resetGame;
    }

    public void setGameAsReset(){
        resetGame = false;
    }
}
