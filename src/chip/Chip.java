package chip;

import java.io.*;
import java.util.Random;

public class Chip {

    //Memoria del Chip8
    private char[] memory;

    //I registri del Chip8
    private char[] V;

    //Puntatore agli indirizzi
    private short I;

    //Program counter
    private char pc;

    private char[] stack;
    private int stackPointer;

    private byte delay_timer;
    private byte sound_timer;

    private byte[] keys;

    //Lo schermo, grande 64 * 32 pixel
    private byte[] display;

    private boolean needRedraw;
    private boolean doSound;

    public void init() {
        memory = new char[4096];
        V = new char[16];
        I = 0x0;

        //Il program counter inizia a 0x200 (512 bytes)
        //perché i primi 512 bytes sono occupati dall'interprete
        //stesso di Chip8 nella macchina.
        //I programmi quindi inizieranno qui
        pc = 0x200;

        stack = new char[16];
        stackPointer = 0;

        delay_timer = 0;
        sound_timer = 0;

        keys = new byte[16];

        //Dimensioni dello schermo
        display = new byte[64 * 32];
        needRedraw = false;

        doSound = false;

        loadFontset();
    }

    public void reset() {
        V = new char[16];
        I = 0x0;

        //Il program counter inizia a 0x200 (512 bytes)
        //perché i primi 512 bytes sono occupati dall'interprete
        //stesso di Chip8 nella macchina.
        //I programmi quindi inizieranno qui
        pc = 0x200;

        stack = new char[16];
        stackPointer = 0;

        delay_timer = 0;
        sound_timer = 0;

        keys = new byte[16];

        //Dimensioni dello schermo
        display = new byte[64 * 32];
        needRedraw = false;

        doSound = false;
    }

    public void run() {
        //Otteniamo l'Opcode
        /*
            Per prima cosa, otteniamo l'Opcode
            L'Opcode è grande 16 bit, ma gli indirizzi di memoria
            sono grandi 8 bit. Cosa facciamo?
            Chiamiamo il primo indirizzo ed il successivo, e li
            uniamo insieme in questo modo:

            - Shiftiamo il primo indirizzo (in posizione pc)
              di 8 bit a sinistra, in modo tale da creare
              un binario a 16 cifre
            - Facciamo l'OR logico di questo primo indirizzo
              con quello successivo

            Se ad esempio abbiamo questi due indirizzi:
                memory[pc] = 0101 0110
                memory[pc+1] = 1001 0000
            faremo lo shift a sinistra di 8 bit del primo valore:
                0101 0110 0000 0000
            poi usiamo l'or per inserire il valore del secondo
            indirizzo nelle ultime otto cifre del primo indirizzo
                0101 0110 1001 0000
         */
        char opcode = (char) ((memory[pc] << 8) | memory[pc + 1]);
        System.out.print(Integer.toHexString(opcode).toUpperCase() + ": ");

        //Decifriamo l'Opcode e lo eseguiamo
        /*
            Useremo un gigantesco switch, dove l'opcode
            fa un & logico con 0xF000, in modo tale da
            considerare solamente il primo nibble
            dell'Opcode, per sapere di che tipo di
            istruzione stiamo parlando
         */
        switch (opcode & 0xF000) {
            case 0x0000: {
                switch (opcode & 0x00FF) {
                    case 0x00E0: //00E0: Clears the screen.
                        for (int i = 0; i < display.length; i++) {
                            display[i] = 0;
                        }

                        System.out.println("Screen cleared");
                        needRedraw = true;
                        pc += 2;
                        break;

                    case 0x00EE: //00EE: Returns from a subroutine.
                        stackPointer--;
                        pc = stack[stackPointer];

                        System.out.println("Returning to " + Integer.toHexString(pc).toUpperCase());
                        pc += 2;
                        break;

                    default:
                        unsupportedOpcode();
                        break;
                }
                break;
            }

            case 0x1000: { //1NNN: Jumps to address NNN.
                int nnn = opcode & 0x0FFF;
                pc = (char) nnn;

                System.out.println("Jumping to " + Integer.toHexString(pc).toUpperCase());
                break;
            }

            case 0x2000: { //2NNN: Calls subroutine at NNN.
                stack[stackPointer] = pc;
                stackPointer++;
                //Prendiamo l'indirizzo dagli ultimi 3 nibbles (000)
                pc = (char) (opcode & 0x0FFF);

                System.out.println("Calling " + Integer.toHexString(pc).toUpperCase());
                break;
            }

            case 0x3000: { //3XNN: Skips the next instruction if VX equals NN.
                int x = (opcode & 0x0F00) >> 8;
                int nn = (opcode & 0x00FF);
                if (V[x] == nn) {
                    pc += 4;
                    System.out.println("Skipping next instruction (V[" + x + "] == " + nn + ")");
                } else {
                    pc += 2;
                    System.out.println("Not skipping next instruction (V[" + x + "] != " + nn + ")");
                }
                break;
            }

            case 0x4000: { //4XNN: Skips the next instruction if VX doesn't equal NN.
                int x = (opcode & 0x0F00) >> 8;
                int nn = (opcode & 0x00FF);
                if (V[x] != nn) {
                    pc += 4;
                    System.out.println("Skipping next instruction (V[" + x + "] != " + nn + ")");
                } else {
                    pc += 2;
                    System.out.println("Not skipping next instruction (V[" + x + "] == " + nn + ")");
                }
                break;
            }

            case 0x5000: {
                switch (opcode & 0x000F){
                    case 0x0000: { //5XY0: Skips the next instruction if VX equals VY.
                        int x = (opcode & 0x0F00) >> 8;
                        int y = (opcode & 0x00F0) >> 4;
                        if (V[x] == V[y]) {
                            pc += 4;
                            System.out.println("Skipping next instruction (V[" + x + "] == (V[" + y + "])");
                        } else {
                            pc += 2;
                            System.out.println("Not skipping next instruction (V[" + x + "] != (V[" + y + "])");
                        }
                        break;
                    }

                    case 0x0001: { //5XY1: COSMAC ELF: Skip the next instruction if register VX is greater than VY.
                        int x = (opcode & 0x0F00) >> 8;
                        int y = (opcode & 0x00F0) >> 4;
                        if (V[x] > V[y]) {
                            pc += 4;
                            System.out.println("COSMAC ELF: Skipping next instruction (V[" + x + "] > (V[" + y + "])");
                        } else {
                            pc += 2;
                            System.out.println("COSMAC ELF: Not skipping next instruction (V[" + x + "] <= (V[" + y + "])");
                        }
                        break;
                    }

                    case 0x0002: { //5XY2: COSMAC ELF: Skip the next instruction if register VX is less than VY.
                        int x = (opcode & 0x0F00) >> 8;
                        int y = (opcode & 0x00F0) >> 4;
                        if (V[x] < V[y]) {
                            pc += 4;
                            System.out.println("COSMAC ELF: Skipping next instruction (V[" + x + "] < (V[" + y + "])");
                        } else {
                            pc += 2;
                            System.out.println("COSMAC ELF: Not skipping next instruction (V[" + x + "] >= (V[" + y + "])");
                        }
                        break;
                    }

                    case 0x0003: { //5XY3: COSMAC ELF: Skip the next instruction if register VX does not equal VY.
                        int x = (opcode & 0x0F00) >> 8;
                        int y = (opcode & 0x00F0) >> 4;
                        if (V[x] != V[y]) {
                            pc += 4;
                            System.out.println("COSMAC ELF: Skipping next instruction (V[" + x + "] != (V[" + y + "])");
                        } else {
                            pc += 2;
                            System.out.println("COSMAC ELF: Not skipping next instruction (V[" + x + "] == (V[" + y + "])");
                        }
                        break;
                    }

                    default:
                        unsupportedOpcode();
                        break;
                }
                break;
            }

            case 0x6000: { //6XNN: Sets VX to NN.
                //Otteniamo l'indice al secondo nibble
                //Shiftiamo a destra di 8 bit (2 nibbles)
                //per rimuovere i bit vuoti ottenuti dall'&
                //(Un nibble equivale a 4 bit)
                int x = (opcode & 0x0F00) >> 8;
                //Inseriamo NN come valore di V[indice]
                V[x] = (char) (opcode & 0x00FF);

                //Avanziamo il programma di due posizioni
                System.out.println("Setting V[" + x + "] to " + (int) V[x]);
                pc += 2;
                break;
            }

            case 0x7000: { //7XNN: Adds NN to VX.
                int x = (opcode & 0x0F00) >> 8;
                int nn = (opcode & 0x00FF);
                //Facciamo l'& per evitare l'overflow
                V[x] = (char) ((V[x] + nn) & 0xFF);

                System.out.println("Adding " + nn + " to V[" + x + "] = " + (int) V[x]);
                pc += 2;
                break;
            }

            case 0x8000: {
                /*
                    Diverse istruzioni iniziano per 0x8, quindi
                    creeremo un altro switch interno
                 */
                switch (opcode & 0x000F) {
                    case 0x0000: { //8XY0: Sets VX to the value of VY.
                        int x = (opcode & 0x0F00) >> 8;
                        int y = (opcode & 0x00F0) >> 4;
                        V[x] = V[y];

                        System.out.println("Setting V[" + x + "] to the value of V[" + y + "]");
                        pc += 2;
                        break;
                    }

                    case 0x0001: { //8XY1: Sets VX to VX or VY.
                        int x = (opcode & 0x0F00) >> 8;
                        int y = (opcode & 0x00F0) >> 4;
                        V[x] = (char) ((V[x] | V[y]) & 0xFF);

                        System.out.println("Setting V[" + x + "] to the value of V[" + x + "] OR V[" + y + "]");
                        pc += 2;
                        break;
                    }

                    case 0x0002: { //8XY2: Sets VX to VX and VY.
                        int x = (opcode & 0x0F00) >> 8;
                        int y = (opcode & 0x00F0) >> 4;
                        V[x] = (char) (V[x] & V[y]);

                        System.out.println("Setting V[" + x + "] to the value of V[" + x + "] AND V[" + y + "]");
                        pc += 2;
                        break;
                    }

                    case 0x0003: { //8XY3: Sets VX to VX xor VY..
                        int x = (opcode & 0x0F00) >> 8;
                        int y = (opcode & 0x00F0) >> 4;
                        V[x] = (char) ((V[x] ^ V[y]) & 0xFF);

                        System.out.println("Setting V[" + x + "] to the value of V[" + x + "] XOR V[" + y + "]");
                        pc += 2;
                        break;
                    }

                    case 0x0004: { //8XY4: Adds VY to VX. VF is set to 1 when there's a carry, and to 0 when there isn't.
                        int x = (opcode & 0x0F00) >> 8;
                        int y = (opcode & 0x00F0) >> 4;
                        //Se V[x] e V[y] sommati formano un numero più grande di 255
                        //allora bisogna impostare il flag V[0x0F] a 1.
                        if (V[y] > 0xFF - V[x]) {
                            V[0xF] = 1;
                            System.out.print("Carry! ");
                        } else {
                            V[0xF] = 0;
                            System.out.print("No carry ");
                        }
                        V[x] = (char) ((V[x] + V[y]) & 0xFF);

                        System.out.println("Adding V[" + x + "] to V[" + y + "] = " + ((V[x] + V[y]) & 0xFF) + ", apply Carry if needed");
                        pc += 2;
                        break;
                    }

                    case 0x0005: { //8XY5: VY is subtracted from VX. VF is set to 0 when there's a borrow, and 1 when there isn't.
                        int x = (opcode & 0x0F00) >> 8;
                        int y = (opcode & 0x00F0) >> 4;
                        //Se V[y] è più grande di V[x], l'operazione andrà sotto lo
                        //zero, quindi si usa il "borrow".
                        if (V[x] > V[y]) {
                            V[0xF] = 1;
                            System.out.print("No borrow. ");
                        } else {
                            V[0xF] = 0;
                            System.out.print("Borrow. ");
                        }
                        V[x] = (char) ((V[x] - V[y]) & 0xFF);

                        System.out.println("Setting V[" + x + "] to the value of V[" + x + "] - V[" + y + "]");
                        pc += 2;
                        break;
                    }

                    case 0x0006: { //8XY6: Stores the least significant bit of VX in VF and then shifts VX to the right by 1.
                        int x = (opcode & 0x0F00) >> 8;
                        //Stiamo ottenendo il bit meno significativo
                        //(Ovvero quello più a destra, usando & 0x1;
                        V[0xF] = (char) (V[x] & 0x1);
                        V[x] = (char) (V[x] >> 1);

                        System.out.println("Store the LSB of V[" + x + "] in VF, then V[" + x + "] >> 1");
                        pc += 2;
                        break;
                    }

                    case 0x0007: { //8XY7: 	Sets VX to VY minus VX. VF is set to 0 when there's a borrow, and 1 when there isn't.
                        int x = (opcode & 0x0F00) >> 8;
                        int y = (opcode & 0x00F0) >> 4;

                        if (V[y] > V[x]) {
                            V[0xF] = 1;
                            System.out.print("No borrow. ");
                        } else {
                            V[0xF] = 0;
                            System.out.print("Borrow. ");
                        }
                        V[x] = (char) ((V[y] - V[x]) & 0xFF);

                        System.out.println("Setting V[" + x + "] to the value of V[" + y + "] - V[" + x + "]");
                        pc += 2;
                        break;
                    }

                    case 0x000E: { //8XYE: Stores the most significant bit of VX in VF and then shifts VX to the left by 1.
                        int x = (opcode & 0x0F00) >> 8;
                        //Stiamo ottenendo il bit più significativo
                        //(Ovvero quello più a sinistra, usando & 0x80;
                        V[0xF] = (char) (V[x] & 0x80);
                        V[x] = (char) (V[x] << 1);

                        System.out.println("Store the MSB of V[" + x + "] in VF, then V[" + x + "] << 1");
                        pc += 2;
                        break;
                    }

                    default:
                        unsupportedOpcode();
                        break;
                }
                break;
            }

            case 0x9000: {
                switch (opcode & 0x000F) {
                    case 0x0000: { //9XY0: 	Skips the next instruction if VX doesn't equal VY.
                        int x = (opcode & 0x0F00) >> 8;
                        int y = (opcode & 0x00F0) >> 4;
                        if (V[x] != V[y]) {
                            pc += 4;
                            System.out.println("Skipping next instruction (V[" + x + "] != (V[" + y + "])");
                        } else {
                            pc += 2;
                            System.out.println("Not skipping next instruction (V[" + x + "] == (V[" + y + "])");
                        }
                        break;
                    }

                    case 0x0001: { //9XY1: COSMAC ELF: Set VF, VX equal to VX multipled by VY where VF is the most significant byte of a 16bit word.
                        int x = (opcode & 0x0F00) >> 8;
                        int y = (opcode & 0x00F0) >> 4;

                        char z = (char) (V[x] * V[y]);

                        V[x] = (char) (z & 0xFF);
                        V[0xF] = (char) ((z >> 8) & 0xFF);

                        System.out.println("COSMAC ELF: Setting V[" + x + "] as (V[" + x + "] * (V[" + y + "]), and V[0xF] as the most significant byte of the result");
                        pc += 2;
                        break;
                    }

                    case 0x0002: { //9XY2: COSMAC ELF: Set VX equal to VX divided by VY. VF is set to the remainder.
                        int x = (opcode & 0x0F00) >> 8;
                        int y = (opcode & 0x00F0) >> 4;

                        V[0xF] =(char) (V[x] % V[y]);
                        V[x] = (char) (V[x] / V[y]);

                        System.out.println("COSMAC ELF: Setting V[" + x + "] as (V[" + x + "] / (V[" + y + "]), and V[0xF] as the remainder");
                        pc += 2;
                        break;
                    }

                    case 0x0003: { //9XY3: COSMAC ELF: Let VX, VY be treated as a 16bit word with VX the most significant part. Convert that word to BCD and store the 5 digits at memory location I through I+4. I does not change.
                        int x = (opcode & 0x0F00) >> 8;
                        int y = (opcode & 0x00F0) >> 4;

                        int word =  ((V[x] << 8) | V[y]);

                        int one = (word - (word % 10000)) / 10000;
                        word -= one * 10000;
                        int two = (word - (word % 1000)) / 1000;
                        word -= two * 1000;
                        int three = (word - (word % 100)) / 100;
                        word -= three * 100;
                        int four = (word - (word % 10)) / 10;
                        word -= four * 10;

                        memory[I] = (char) one;
                        memory[I + 1] = (char) two;
                        memory[I + 2] = (char) three;
                        memory[I + 3] = (char) four;
                        memory[I + 4] = (char) word;

                        System.out.println("COSMAC ELF: Storing Binary-Coded Decimal (V[" + x + "] << 8 |  = V[" + y + "]) = " + word + " as {" + one + ", " + two + ", " + three + ", " + four + ", " + word + "}");
                        pc += 2;
                        break;
                    }

                    default:
                        unsupportedOpcode();
                        break;
                }
                break;
            }

            case 0xA000: { //ANNN: Sets I to the address NNN.
                I = (short) (opcode & 0x0FFF);

                System.out.println("Set I to " + Integer.toHexString(I).toUpperCase());
                pc += 2;
                break;
            }

            case 0xB000: { //BNNN: Jumps to the address NNN plus V0.
                int nnn = (char) (opcode & 0x0FFF);
                int extra = V[0] & 0xFF;
                pc = (char) (extra + nnn);

                System.out.println("Jump to " + nnn + " + " + V[0]);
                break;
            }

            case 0xC000: { //CXNN: Sets VX to the result of a bitwise and operation on a random number (Typically: 0 to 255) and NN.
                int x = (opcode & 0x0F00) >> 8;
                int nn = (opcode & 0x00FF);
                int randomNumber = new Random().nextInt(255) & nn;
                V[x] = (char) randomNumber;

                System.out.println("V[" + x + "] has been set to (randomised) " + randomNumber);
                pc += 2;
                break;
            }

            case 0xD000: { //DXYN: Draw a sprite (X, Y) size (8, N). Sprite is located at I
                int x = V[(opcode & 0x0F00) >> 8];
                int y = V[(opcode & 0x00F0) >> 4];
                int height = opcode & 0x000F;

                V[0xF] = 0;

                for (int _y = 0; _y < height; _y++) {
                    int line = memory[I + _y];
                    for (int _x = 0; _x < 8; _x++) {
                        int pixel = line & (0x80 >> _x);
                        if (pixel != 0) {
                            int totalX = x + _x;
                            int totalY = y + _y;

                            //Codice di wrapping, per evitare che
                            //l'indice vada outofbounds
                            totalX = totalX % 64;
                            totalY = totalY % 32;

                            int index = (totalY * 64) + totalX;

                            if (display[index] == 1)
                                V[0xF] = 1;

                            display[index] ^= 1;
                        }
                    }
                }

                System.out.println("Drawing at V[" + ((opcode & 0x0F00) >> 8) + "] = " + x + ", V[" + ((opcode & 0x00F0) >> 4) + "] = " + y);
                pc += 2;
                needRedraw = true;
                break;
            }

            case 0xE000: {
                switch (opcode & 0x00FF) {
                    case 0x009E: { //EX9E: Skips the next instruction if the key stored in VX is pressed.
                        int x = (opcode & 0x0F00) >> 8;
                        int key = V[x];
                        if (keys[key] == 1) {
                            System.out.println("Skipping next instruction if V[" + x + "] = " + (int) V[x] + " is pressed");
                            pc += 4;
                        } else {
                            System.out.println("Not skipping next instruction if V[" + x + "] = " + (int) V[x] + " is NOT pressed");
                            pc += 2;
                        }
                        break;
                    }

                    case 0x00A1: //EXA1: Skips the next instruction if the key stored in VX isn't pressed.
                        int x = (opcode & 0x0F00) >> 8;
                        int key = V[x];
                        if (keys[key] == 0) {
                            System.out.println("Skipping next instruction if V[" + x + "] = " + (int) V[x] + " is NOT pressed");
                            pc += 4;
                        } else {
                            System.out.println("Not skipping next instruction if V[" + x + "] = " + (int) V[x] + " is pressed");
                            pc += 2;
                        }
                        break;

                    default:
                        unsupportedOpcode();
                        break;
                }
                break;
            }

            case 0xF000: {
                switch (opcode & 0x00FF) {
                    case 0x007: { //FX07: Sets VX to the value of the delay timer.
                        int x = (opcode & 0x0F00) >> 8;
                        V[x] = (char) delay_timer;

                        System.out.println("Setting V[" + x + "] to delay_timer value " + delay_timer);
                        pc += 2;
                        break;
                    }

                    case 0x00A: { //FX0A: A key press is awaited, and then stored in VX.

                        int x = (opcode & 0x0F00) >> 8;
                        for (int i = 0; i < keys.length; i++) {
                            if (keys[i] == 1) {
                                V[x] = (char) i;
                                pc += 2;
                                break;
                            }
                        }

                        System.out.println("Awaiting key press to be stored in V[" + x + "]");
                        break;
                    }

                    case 0x015: { //FX15: Sets the delay timer to VX
                        int x = (opcode & 0x0F00) >> 8;
                        delay_timer = (byte) V[x];

                        System.out.println("Setting delay_timer to V[" + x + "] = " + (int) V[x]);
                        pc += 2;
                        break;
                    }

                    case 0x018: { //FX18: Sets the sound timer to VX
                        int x = (opcode & 0x0F00) >> 8;
                        sound_timer = (byte) V[x];

                        System.out.println("Setting sound_timer to V[" + x + "] = " + (int) V[x]);
                        pc += 2;
                        break;
                    }

                    case 0x01E: { //FX1E: Adds VX to I. VF is not affected. (or maybe yes?)
                        int x = (opcode & 0x0F00) >> 8;
                        V[0xF] = (char) ((I + V[x] > 0xfff) ? 1 : 0);
                        I = (short) (I + V[x]);

                        System.out.println("Adding V[" + x + "] with the value of " + (int) V[x] + " to I");
                        pc += 2;
                        break;
                    }

                    case 0x029: { //FX29: Sets I to the location of the sprite for the character VX (Fontset)
                        int x = (opcode & 0x0F00) >> 8;
                        int character = V[x];
                        I = (short) (0x050 + (character * 5));

                        System.out.println("Setting I to Character V[" + x + "] = " + (int) V[x] + " Offset to 0x" + Integer.toHexString(I).toUpperCase());
                        pc += 2;
                        break;
                    }

                    case 0x033: { //FX33: Store a binary-coded decimal value VX in I, I + 1 and I + 2
                        int x = (opcode & 0x0F00) >> 8;
                        //Otteniamo il numero e lo salviamo come decimale
                        int value = V[x];

                        //Otteniamo centinaio, decina ed unità dal numero
                        int hundreds = (value - (value % 100)) / 100;
                        value -= hundreds * 100;
                        int tens = (value - (value % 10)) / 10;
                        value -= tens * 10;

                        //Li salviamo in I, I+1 e I+2
                        memory[I] = (char) hundreds;
                        memory[I + 1] = (char) tens;
                        memory[I + 2] = (char) value;

                        System.out.println("Storing Binary-Coded Decimal V[" + x + "] = " + value + " as {" + hundreds + ", " + tens + ", " + value + "}");
                        pc += 2;
                        break;
                    }

                    case 0x055: { //FX55: Stores V0 to VX (including VX) in memory starting at address I. The offset from I is increased by 1 for each value written, but I itself is left unmodified.
                        int x = (opcode & 0x0F00) >> 8;
                        for (int i = 0; i < x; i++) {
                            memory[I + i] = V[i];
                        }

                        //Nell'interprete originale, I viene modificato
                        //ma useremo il comportamento delle versioni successive (SUPER CHIP-8)
                        //I += x + 1;

                        System.out.println("Storing V[0] to V[" + x + "] to the values of memory[0x" + Integer.toHexString(I & 0xFFFF).toUpperCase() + "]");
                        pc += 2;
                        break;
                    }

                    case 0x065: { //FX65: Fills V0 to VX (including VX) with values from memory starting at address I.
                        int x = (opcode & 0x0F00) >> 8;
                        for (int i = 0; i <= x; i++) {
                            V[i] = memory[I + i];
                        }

                        //Nell'interprete originale, I viene modificato
                        //ma useremo il comportamento delle versioni successive (SUPER CHIP-8)
                        //I += x + 1;

                        System.out.println("Setting V[0] to V[" + x + "] to the values of memory[0x" + Integer.toHexString(I & 0xFFFF).toUpperCase() + "]");
                        pc += 2;
                        break;
                    }

                    case 0x094: { //FX94: COSMAC ELF: Load I with the font sprite of the 6-bit ASCII value found in VX; V0 is set to the symbol length
                        int x = (opcode & 0x0F00) >> 8;

                        int c = V[x]*3 +0x100;

                        int ab = memory[c];
                        int cd = memory[c + 1];
                        int ef = memory[c + 2];

                        memory[0x1C0] = memory[0xF0 + (ef & 0xF)];
                        memory[0x1C1] = memory[0xF0 + (cd >> 4)];
                        memory[0x1C2] = memory[0xF0 + (cd & 0xF)];
                        memory[0x1C3] = memory[0xF0 + (ab >> 4)];
                        memory[0x1C4] = memory[0xF0 + (ab & 0xF)];

                        V[0] = (char) (ef >> 4);

                        I = 0x1C0;

                        System.out.println("COSMAC ELF: Loading I with font sprite from the value of V[" + x + "]");
                        pc += 2;
                        break;
                    }

                    default:
                        unsupportedOpcode();
                        break;
                }
                break;
            }


            default:
                unsupportedOpcode();
                break;
        }

        if (sound_timer > 0)
            --sound_timer;
        if (delay_timer > 0)
            --delay_timer;

        if (sound_timer == 1) {
            doSound = true;
        }
    }

    private void unsupportedOpcode(){
        System.err.println("Unsupported Opcode!");
        System.exit(0);
    }

    public byte[] getDisplay() {
        return display;
    }

    public boolean needsRedraw() {
        return needRedraw;
    }

    public void removeDrawFlag() {
        needRedraw = false;
    }

    public boolean needsSound() {
        return doSound;
    }

    public void removeSoundFlag() {
        doSound = false;
    }

    public void loadFontset() {
        for (int i = 0; i < ChipData.fontset.length; i++) {
            memory[0x50 + i] = (char) (ChipData.fontset[i] & 0xFF);
        }
    }

    public void loadProgram(String file) {
        DataInputStream input = null;
        try {
            input = new DataInputStream(new FileInputStream(new File(file)));

            int offset = 0;
            while (input.available() > 0) {
                memory[0x200 + offset] = (char) (input.readByte() & 0xFF);
                offset++;
            }

        } catch (IOException e) {
            e.printStackTrace();
            System.exit(0);
        } finally {
            if (input != null) {
                try {
                    input.close();
                } catch (IOException ex) {
                    ex.printStackTrace();
                    System.exit(0);
                }
            }
        }
    }

    public void loadProgram(File file) {
        if (file == null) {
            System.err.println("File not found");
            System.exit(0);
        }
        try (DataInputStream input = new DataInputStream(new FileInputStream(file))) {

            int offset = 0;
            while (input.available() > 0) {
                memory[0x200 + offset] = (char) (input.readByte() & 0xFF);
                offset++;
            }

        } catch (IOException e) {
            e.printStackTrace();
            System.exit(0);
        }
    }

    public void setKeyBuffer(int[] keyBuffer) {
        if (keyBuffer == null) {
            return;
        }
        for (int i = 0; i < keys.length; i++) {
            keys[i] = (byte) keyBuffer[i];
        }
    }
}
