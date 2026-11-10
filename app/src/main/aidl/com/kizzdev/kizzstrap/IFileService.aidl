package com.kizzdev.kizzstrap;

interface IFileService {
    // Wajib: id transaksi 16777114 dipakai Shizuku untuk menutup service
    void destroy() = 16777114;

    String listDir(String path) = 1;
    String readFile(String path) = 2;
    String writeFile(String path, String content) = 3;
    String copyFile(String from, String to) = 4;
    String findFiles(String root, String name, int maxDepth) = 5;
    String deleteRecursive(String path, boolean keepRoot) = 6;
    String exec(String command) = 7;
    long sizeOf(String path) = 8;
}
