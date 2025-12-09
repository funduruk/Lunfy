package ru.funduruk;

public class Main {
    public static void main(String[] args) throws InterruptedException {
        new NettyServer(9000).start();
    }
}