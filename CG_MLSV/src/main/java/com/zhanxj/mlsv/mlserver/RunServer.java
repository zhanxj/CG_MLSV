package com.zhanxj.mlsv.mlserver;

import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.zhanxj.mlsv.entries.DuelRank;
import com.zhanxj.mlsv.entries.GoldRank;
import com.zhanxj.mlsv.entries.Mails;
import com.zhanxj.mlsv.entries.ServerList;
import com.zhanxj.mlsv.utils.CommonConst;

/**
 * 启动GMSV线程
 * 
 * @author Hokin.jim
 * 
 */
public class RunServer {
	

	private static Logger LOG = LoggerFactory.getLogger(RunServer.class);
	private  final int port =CommonConst.PORT;
	private final ServerSocket serverSocket;
	private final ExecutorService executorService;// 线程池
	private final int POOL_SIZE = 5;// 单个CPU线程池大小
	private ServerList sl = new ServerList();
	private Mails mails = new Mails();
	private GoldRank gr = new GoldRank();
	private DuelRank dr = new DuelRank();

	public RunServer() throws IOException {
		serverSocket = new ServerSocket(port);
		// Runtime的availableProcessor()方法返回当前系统的CPU数目.
		executorService = Executors.newFixedThreadPool(Runtime.getRuntime()
				.availableProcessors() * POOL_SIZE);
		LOG.info("Crossgate MLSV Start。。。 port:{}",port);
	}

	public void run() {
		while (true) {
			Socket socket = null;
			try {
				// 接收客户连接,只要客户进行了连接,就会触发accept();从而建立连接
				socket = serverSocket.accept();
				executorService.execute(new ServerThread(socket, sl, mails, gr, dr));
			} catch (Exception e) {
				LOG.error(e.toString());
			}
		}
	}

	public static void main(final String[] args) throws IOException {
		new RunServer().run();
	}

}
