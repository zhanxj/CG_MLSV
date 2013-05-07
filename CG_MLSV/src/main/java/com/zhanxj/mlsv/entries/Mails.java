package com.zhanxj.mlsv.entries;

import java.io.File;
import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.io.Files;
import com.zhanxj.mlsv.utils.CommonConst;

public class Mails {
	private static Logger LOG = LoggerFactory.getLogger(Mails.class);
	private ArrayList<Mail> mails = null;
	private final String MAIL_DB = CommonConst.MAIL_DB;

	public Mails() {
		mails = new ArrayList<Mail>();
		this.loadFromFile();
	}

	public void addMail(Mail m) {
		if (mails != null && !mails.contains(m)) {
			mails.add(m);
		}
	}

	public ArrayList<Mail> findMails(String tc, int tr) {
		ArrayList<Mail> Ret = new ArrayList<Mail>();

		for (int i = 0; i < mails.size(); i++) {
			Mail m = mails.get(i);
			if (m != null && m.getToCdkey().equals(tc)
					&& m.getToRegNumber() == tr) {
				if (!Ret.contains(m))
					Ret.add(m);
			}

		}
		if (Ret.size() > 0) {
			for (int i = 0; i < Ret.size(); i++) {
				Mail m = Ret.get(i);
				if (mails.contains(m))
					mails.remove(m);
			}
		}
		return Ret;
	}

	public void loadFromFile() {
		File sysfile = new File(MAIL_DB);
		if (sysfile.exists()) {
			try {
				List<String> Strings = Files.readLines(sysfile, Charset.forName("GB2312"));
				for (String line : Strings) {
					String[] buf = line.split("	");
					Mail m = new Mail(buf);
					this.addMail(m);
				}
				LOG.info("Load {} buffered mails info.", this.mails.size());
			} catch (IOException e) {
				LOG.error(e.toString());
			}
		}
	}

	public void writeToFile() {
		if (this.mails == null || this.mails.size() == 0)
			return;
		File sysfile = new File(MAIL_DB);
		String buf = "";
		for (int i = 0; i < this.mails.size(); i++) {
			Mail m = this.mails.get(i);
			if (m != null) {
				buf += i + "	" + m.getFromCdkey() + "	"
						+ m.getFromRegNumber() + "	" + m.getToCdkey() + "	"
						+ m.getToRegNumber() + "	" + m.getMsg() + "	"
						+ m.getUnk1() + "	" + m.getUnk2() + "	"
						+ m.getUnk3() + "\r\n";
			}
		}
		try {
			Files.write(buf.getBytes("GB2312"), sysfile);
		} catch (UnsupportedEncodingException e) {
			LOG.error(e.toString());
		} catch (IOException e) {
			LOG.error(e.toString());
		}
	}
}
