package com.gasimo;

import com.sun.jna.Memory;
import com.sun.jna.Pointer;
import com.sun.jna.ptr.IntByReference;
import com.sun.jna.win32.StdCallLibrary;

public interface Kernel32 extends StdCallLibrary {

    Pointer GetStdHandle(IntByReference nStdHandle);

    Boolean SetConsoleTextAttribute(Pointer hConsoleOutput, IntByReference wAttributes);

    /* derp */
    int GetLastError();
}