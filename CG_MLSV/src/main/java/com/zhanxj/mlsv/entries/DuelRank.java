package com.zhanxj.mlsv.entries;

import java.io.File;
import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.io.Files;
import com.zhanxj.mlsv.utils.CommonConst;
import com.zhanxj.mlsv.utils.RankComparator;
import com.zhanxj.mlsv.utils.Tools;

public class DuelRank {
	private static Logger LOG = LoggerFactory.getLogger(DuelRank.class);
	private ArrayList<Rank> duelrank = null;
	private final String DUELRANK_DB = CommonConst.DUELRANK_DB;

	public DuelRank() {
		duelrank = new ArrayList<Rank>();
	}

	public void RankSort() {
		if (duelrank != null && duelrank.size() > 1) {
			Comparator<Rank> comp = new RankComparator();
			Collections.sort(duelrank, comp);
		}
	}

	public Rank getRank(String cdkey, int rn) {
		Rank ret = null;
		if (duelrank != null && duelrank.size() > 0) {
			for (int i = 0; i < duelrank.size(); i++) {
				ret = duelrank.get(i);
				if (ret != null && ret.getCdkey().equals(cdkey)
						&& ret.getRegNumber() == rn)
					return ret;
				else
					ret = null;
			}
		}
		return ret;
	}

	public int getPlayerRankInfo(String cdkey, int rn) {
		int ret = -1;
		this.RankSort();
		if (duelrank != null && duelrank.size() > 0) {
			for (int i = 0; i < duelrank.size(); i++) {
				Rank r = duelrank.get(i);
				if (r != null && r.getCdkey().equals(cdkey)
						&& r.getRegNumber() == rn) {
					ret = i;
					break;
				}
			}
		}

		return ret;
	}

	public String getLimitRankInfo(int start) {
		String ret = "";

		this.RankSort();

		if (start >= duelrank.size())
			start = 0;

		if (duelrank != null && duelrank.size() > 0) {
			int count = 0;
			for (int i = start; i < duelrank.size(); i++) {
				Rank r = duelrank.get(i);
				if (r != null) {
					ret = ret + "10," + count + "," + r.getCdkey() + "#"
							+ Tools.sixtyTwoScale(r.getRegNumber()) + ","
							+ r.getValue() + "," + r.getStr() + "|";
					count++;
				}
			}
		}

		return ret;
	}

	public void delRank(Rank r) {
		if (r != null && duelrank.contains(r)) {
			duelrank.remove(r);
		}
	}

	public void addRank(Rank r) {
		if (r != null && !duelrank.contains(r)) {
			duelrank.add(r);
		}
	}

	public void loadFromFile() {
		File sysfile = new File(DUELRANK_DB);
		if (sysfile.exists()) {
			try {
				List<String> Strings = Files.readLines(sysfile,
						Charset.forName("GB2312"));
				for (String line : Strings) {
					String[] buf = line.split("	");
					Rank r = new Rank(buf);
					this.addRank(r);
				}
				LOG.info("Load {} buffered duel_rank info.",
						this.duelrank.size());
			} catch (IOException e) {
				LOG.error(e.toString());
			}
		}
	}

	public void writeToFile() {
		if (this.duelrank == null || this.duelrank.size() == 0)
			return;
		File sysfile = new File(DUELRANK_DB);
		String buf = "";
		for (int i = 0; i < this.duelrank.size(); i++) {
			Rank r = this.duelrank.get(i);
			if (r != null) {
				buf += i + "	" + r.getTable() + "	" + r.getCdkey() + "	"
						+ r.getRegNumber() + "	" + r.getValue() + "	"
						+ r.getStr() + "	" + r.getUnk1() + "	" + r.getUnk2()
						+ "\r\n";
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
