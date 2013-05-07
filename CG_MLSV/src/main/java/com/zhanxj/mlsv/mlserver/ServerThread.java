package com.zhanxj.mlsv.mlserver;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.PrintWriter;
import java.net.Socket;
import java.util.ArrayList;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.zhanxj.mlsv.entries.DuelRank;
import com.zhanxj.mlsv.entries.GmsvServer;
import com.zhanxj.mlsv.entries.GoldRank;
import com.zhanxj.mlsv.entries.Mail;
import com.zhanxj.mlsv.entries.Mails;
import com.zhanxj.mlsv.entries.Player;
import com.zhanxj.mlsv.entries.Rank;
import com.zhanxj.mlsv.entries.ServerList;
import com.zhanxj.mlsv.utils.CommonConst;
import com.zhanxj.mlsv.utils.Tools;

public class ServerThread implements Runnable {

	private static Logger LOG = LoggerFactory.getLogger(ServerThread.class);
	
	public void run() {
		try {
			final BufferedReader br = getReader(socket);
			final PrintWriter pw = getWriter(socket);
			String msg = null;
			while ((msg = br.readLine()) != null) {
				String[] ret = echo(msg);
				if (ret != null) {
					int l = ret.length;
					if (l == 1) {
						pw.println(new String(ret[0].getBytes("ISO8859_1"),"GBK"));
					} else {
						for (int i = 0; i < l; i++) {
							pw.println(new String(ret[i].getBytes("ISO8859_1"),"GBK"));
						}
					}
				}
			}
		} catch (IOException e) {
			LOG.error(e.toString());
		} finally {
			try {
				if (socket != null)
					socket.close();
			} catch (IOException e) {
				LOG.error(e.toString());
			}
		}

	}

	private final String TBL_GOLD = CommonConst.TBL_GOLD;
	private final String TBL_DUEL = CommonConst.TBL_DUEL;
	private final Socket socket;
	private final String connectPassword = CommonConst.CONNECTPASSWORD;
	private ServerList sl;
	private Mails ml;
	private GoldRank gr;
	private DuelRank dr;

	public ServerThread(final Socket socket, ServerList sl, Mails ml,
			GoldRank gr, DuelRank dr) {
		this.socket = socket;
		this.sl = sl;
		this.ml = ml;
		this.gr = gr;
		this.dr = dr;
	}

	private PrintWriter getWriter(final Socket socket) throws IOException {
		final OutputStream socketOut = socket.getOutputStream();
		return new PrintWriter(socketOut, true);
	}

	private BufferedReader getReader(final Socket socket) throws IOException {
		final InputStream socketIn = socket.getInputStream();
		return new BufferedReader(new InputStreamReader(socketIn,"GBK"));
	}

	public String[] echo(final String msg) {
		String[] ret = null;
		String myIP = socket.getInetAddress().toString();
		String myPort = Integer.toString(socket.getPort());
		if (msg != null) {
			String[] packet = msg.split(" ");
			if (vaildPacketHeader(packet[0])) {
				if (packet[0].equalsIgnoreCase("ACServerLogin")) {
					if (packet[2] != null && packet[2].equals(connectPassword)) {
						GmsvServer gs = new GmsvServer();
						gs.setServerName(packet[1]);
						gs.setServerID(Integer.parseInt(packet[3]));
						gs.setServerIP(myIP);
						gs.setServerPort(myPort);
						sl.addNewServer(gs);
						ret = new String[1];
						ret[0] = "ACServerLogin successful";
						Object[] logArgs=new Object[4];
						logArgs[0]=packet[3];
						logArgs[1]=packet[1];
						logArgs[2]=myIP;
						logArgs[3]=myPort;
						LOG.info("Authed Server [{}:{}] connected... From {}:{}",logArgs);
					}
				} else if (packet[0].equalsIgnoreCase("ACServerLogout")) {
					ret = null;
				} else if (packet[0].equalsIgnoreCase("DBUpdateEntryString")) {
					if (packet[1].equalsIgnoreCase("db_addressbook")) {
						Player p = null;
						String cdkey = packet[2].split("#")[0];
						int RegNumber = Tools.sixtyTwoScale(packet[2]
								.split("#")[1]);

						String buf = packet[3];
						String[] bufInfo = buf.split("\\|");
						p = sl.getPlayer(cdkey, RegNumber);
						if (p != null) {
							p.setOnline(Integer.parseInt(bufInfo[0]));
							p.setLevel(Integer.parseInt(bufInfo[1]));
							p.setTitleName(bufInfo[2]);
							p.setFaceNumber(Integer.parseInt(bufInfo[3]));
							p.setIndex(packet[4]);
						} else {
							p = new Player();
							p.setCdkey(cdkey);
							p.setRegNumber(RegNumber);
							p.setOnline(Integer.parseInt(bufInfo[0]));
							p.setLevel(Integer.parseInt(bufInfo[1]));
							p.setTitleName(bufInfo[2]);
							p.setFaceNumber(Integer.parseInt(bufInfo[3]));
							p.setIndex(packet[4]);
							sl.addPlayerToServer(p, p.getOnline());
						}
						ret = new String[1];
						ret[0] = p.makeDBGetEntryString();
					} else if (packet[1].equalsIgnoreCase("db_guild")) {
						/*
						 * String str = packet[2].split("#")[0]; int num =
						 * Integer.parseInt(packet[2].split("#")[1]);
						 */
					}
				} else if (packet[0].equalsIgnoreCase("DBGetEntryString")) {
					if (packet[1].equalsIgnoreCase("db_addressbook")) {
						Player p = null;
						String cdkey = packet[2].split("#")[0];
						int RegNumber = Tools.sixtyTwoScale(packet[2]
								.split("#")[1]);
						p = sl.getPlayer(cdkey, RegNumber);
						if (p != null) {
							p.setFdIndex(packet[3]);
							p.setIndex(packet[4]);
							ret = new String[1];
							ret[0] = p.makeDBGetEntryString();
						}

					}
				} else if (packet[0].equalsIgnoreCase("Broadcast")) {
					ret = new String[1];
					ret[0] = packet[0] + " " + packet[1] + " " + packet[2]
							+ " " + packet[3];
				} else if (packet[0].equalsIgnoreCase("Message")) {
					if (packet[2].trim().equals("-1")
							&& packet[5].startsWith("P|"))
					{

					} else if (packet[2].trim().equals("-1")
							&& !packet[5].startsWith("P|"))
					{

					} else if (!packet[2].trim().equals("-1"))
					{
						Mail m = new Mail(packet);
						ml.addMail(m);
						ml.writeToFile();
					}
				} else if (packet[0].equalsIgnoreCase("ACUCheckReq"))
				{
					int RegNumber = Tools.sixtyTwoScale(packet[1]);
					String cdkey = packet[2];
					GmsvServer gs = sl.getServerFromIP(myIP, myPort);
					// System.out.println("ACUCheckReq "+RegNumber+" " + cdkey);
					if (gs.findPlayer(cdkey, RegNumber) != null
							&& gs.findPlayer(cdkey, RegNumber).getOnline() == 1) {
						// System.out.println("ACUCheckReq sent");
						ret = new String[1];
						ret[0] = "ACUCheck " + cdkey;
					}
				} else if (packet[0].equalsIgnoreCase("MessageFlush")) {
					// System.out.println("in MessageFlush");
					ArrayList<Mail> m = ml.findMails(packet[1],
							Tools.sixtyTwoScale(packet[2]));
					if (m != null && m.size() > 0) {
						ret = new String[m.size()];
						for (int i = 0; i < m.size(); i++) {
							ret[i] = m.get(i).getMailPacket();
						}
						ml.writeToFile();
					}
				} else if (packet[0].equalsIgnoreCase("DBUpdateEntryInt")) {
					if (packet[1].equals(TBL_GOLD)) {
						String cdkey = packet[2].split("#")[0];
						int RegNumber = Tools.sixtyTwoScale(packet[2]
								.split("#")[1]);
						Rank r = gr.getRank(cdkey, RegNumber);
						if (r == null) {
							r = new Rank();
							r.setTable(TBL_GOLD);
							r.setCdkey(cdkey);
							r.setRegNumber(RegNumber);
							r.setValue(Tools.sixtyTwoScale(packet[3]));
							r.setStr(packet[4]);
							r.setUnk1(Tools.sixtyTwoScale(packet[5]));
							r.setUnk2(Tools.sixtyTwoScale(packet[6]));
							gr.addRank(r);
						} else {
							r.setTable(TBL_GOLD);
							r.setCdkey(cdkey);
							r.setRegNumber(RegNumber);
							r.setValue(Tools.sixtyTwoScale(packet[3]));
							r.setStr(packet[4]);
							r.setUnk1(Tools.sixtyTwoScale(packet[5]));
							r.setUnk2(Tools.sixtyTwoScale(packet[6]));
						}
						ret = new String[1];
						ret[0] = "DBUpdateEntryInt successful " + packet[2]
								+ " " + TBL_GOLD + " " + packet[5] + " "
								+ packet[6];
						gr.writeToFile();
					} else if (packet[1].equals(TBL_DUEL)) {
						String cdkey = packet[2].split("#")[0];
						int RegNumber = Tools.sixtyTwoScale(packet[2]
								.split("#")[1]);
						Rank r = dr.getRank(cdkey, RegNumber);
						if (r == null) {
							r = new Rank();
							r.setTable(TBL_DUEL);
							r.setCdkey(cdkey);
							r.setRegNumber(RegNumber);
							r.setValue(Tools.sixtyTwoScale(packet[3]));
							r.setStr(packet[4]);
							r.setUnk1(Tools.sixtyTwoScale(packet[5]));
							r.setUnk2(Tools.sixtyTwoScale(packet[6]));
							dr.addRank(r);
						} else {
							r.setTable(TBL_DUEL);
							r.setCdkey(cdkey);
							r.setRegNumber(RegNumber);
							r.setValue(Tools.sixtyTwoScale(packet[3]));
							r.setStr(packet[4]);
							r.setUnk1(Tools.sixtyTwoScale(packet[5]));
							r.setUnk2(Tools.sixtyTwoScale(packet[6]));
						}
						ret = new String[1];
						ret[0] = "DBUpdateEntryInt successful " + packet[2]
								+ " " + TBL_DUEL + " " + packet[5] + " "
								+ packet[6];
						dr.writeToFile();
					}
				} else if (packet[0].equalsIgnoreCase("DBDeleteEntryInt")) {
					String cdkey = packet[2].split("#")[0];
					int RegNumber = Tools
							.sixtyTwoScale(packet[2].split("#")[1]);
					Rank r = null;
					ret = new String[1];
					if (packet[1].equals(TBL_GOLD)) {
						r = gr.getRank(cdkey, RegNumber);
						if (r != null) {
							gr.delRank(r);
							ret[0] = "DBDeleteEntryInt successful " + packet[2]
									+ " " + TBL_GOLD + " " + packet[3] + " "
									+ packet[4];
						} else {
							ret[0] = "DBDeleteEntryInt failed " + packet[2]
									+ " " + TBL_GOLD + " " + packet[3] + " "
									+ packet[4];
						}
						gr.writeToFile();
					} else if (packet[1].equals(TBL_DUEL)) {
						r = dr.getRank(cdkey, RegNumber);
						if (r != null) {
							dr.delRank(r);
							ret[0] = "DBDeleteEntryInt successful " + packet[2]
									+ " " + TBL_DUEL + " " + packet[3] + " "
									+ packet[4];
						} else {
							ret[0] = "DBDeleteEntryInt failed " + packet[2]
									+ " " + TBL_DUEL + " " + packet[3] + " "
									+ packet[4];
						}
						dr.writeToFile();
					}
				} else if (packet[0].equalsIgnoreCase("DBGetEntryRank")) { 
					// packet[3] packet[4]
					String cdkey = packet[2].split("#")[0];
					int RegNumber = Tools
							.sixtyTwoScale(packet[2].split("#")[1]);
					if (packet[1].equals(TBL_GOLD)) {// mlsvproto_DBGetEntryRank_recv(v83,
														// a1, sFlg, v123, rank,
														// tName, v80, fd,
														// objIndex);
						ret = new String[1];
						int re = gr.getPlayerRankInfo(cdkey, RegNumber);
						if (re != -1) {
							ret[0] = "DBGetEntryRank successful " + "0" + " "
									+ re + " " + TBL_GOLD + " " + packet[2]
									+ " " + packet[3] + " " + packet[4];
						} else {
							ret[0] = "DBGetEntryRank failed " + "0" + " " + re
									+ " " + TBL_GOLD + " " + packet[2] + " "
									+ packet[3] + " " + packet[4];
						}
					} else if (packet[1].equals(TBL_DUEL)) {
						ret = new String[1];
						int re = dr.getPlayerRankInfo(cdkey, RegNumber);
						if (re != -1) {
							ret[0] = "DBGetEntryRank successful " + "0" + " "
									+ re + " " + TBL_DUEL + " " + packet[2]
									+ " " + packet[3] + " " + packet[4];
						} else {
							ret[0] = "DBGetEntryRank failed " + "0" + " " + re
									+ " " + TBL_DUEL + " " + packet[2] + " "
									+ packet[3] + " " + packet[4];
						}
					}
				} else if (packet[0].equalsIgnoreCase("DBGetEntryByCount")) { 
					if (packet[1].equals(TBL_GOLD)) {
						int from = Tools.sixtyTwoScale(packet[2]);
						ret = new String[1];
						String r = gr.getLimitRankInfo(from);
						if (r != null && !r.equals(""))
							ret[0] = "DBGetEntryByCount successful " + r + " "
									+ TBL_GOLD + " " + packet[3] + " "
									+ packet[4] + " " + packet[5];
						else
							ret[0] = "DBGetEntryByCount failed " + "1" + " "
									+ TBL_GOLD + " " + packet[3] + " "
									+ packet[4] + " " + packet[5];

					} else if (packet[1].equals(TBL_DUEL)) {
						int from = Tools.sixtyTwoScale(packet[2]);
						ret = new String[1];
						String r = dr.getLimitRankInfo(from);
						if (r != null && !r.equals(""))
							ret[0] = "DBGetEntryByCount successful " + r + " "
									+ TBL_DUEL + " " + packet[3] + " "
									+ packet[4] + " " + packet[5];
						else
							ret[0] = "DBGetEntryByCount failed " + "1" + " "
									+ TBL_DUEL + " " + packet[3] + " "
									+ packet[4] + " " + packet[5];

					}
				}
			}
		}
		return ret;
	}

	private boolean vaildPacketHeader(String string) {
		String[] packets = { "ACServerLogin", "DBUpdateEntryString",
				"ACUCheckReq", "ACServerLogout", "Broadcast", "Message",
				"DBGetEntryByCount", "DBDeleteEntryInt", "DBUpdateEntryInt",
				"DBGetEntryRank", "DBGetEntryByRank", "DBDeleteEntryString",
				"DBGetEntryString", "MessageFlush" };
		for (int i = 0; i < packets.length; i++) {
			if (packets[i].equalsIgnoreCase(string))
				return true;
		}
		return false;
	}
}
