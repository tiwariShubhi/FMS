package com.oopd.fms;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.sql.Date;
import java.sql.ResultSet;
import java.sql.Time;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

import com.oopd.dao.DBService;

public class FmsMain {

	public static void main(String[] args) throws IOException {
		// TODO Auto-generated method stub

		
		FbClient fb = new FbClient();
		
		fb.connect();
		fb.readFeed();
		fb.parsePost();
		BufferedReader br = new BufferedReader(new InputStreamReader(System.in));
		System.out.println("DO you wish to see the defaulters?Yes or No");
		String [] s = br.readLine().split("\\s");
		if(s[0].equalsIgnoreCase("yes"))
		{
			checkDefaulters();
		}
		else
		{
			System.out.println("BYE BYE!");
		}
	}

	
	public static void checkDefaulters()
	{
		DBService ob = new DBService();
		String q = "select DATEDIFF(comp_finish_date,comp_log_date),TIMEDIFF(comp_finish_time,comp_log_time),worker_id from complaint;";
		ResultSet rs = ob.selectData(q);
		Date date;
		Time time;
		int d;
		String wid="";
		ArrayList<String> def = new ArrayList<String>();
		HashMap<String,Integer> defaulters = new HashMap<String,Integer>();
		try
		{
			while(rs.next())
			{
				//date = rs.getDate(1);
				d = rs.getInt(1);
				time = rs.getTime(2);
				wid = rs.getString(3);
				if(  time!=null)
				{
					if(d >=1)
					{
						//defaulter
						def.add(wid);
						if(defaulters.containsKey(wid))
						{
							defaulters.put(wid, defaulters.get(wid)+1);
						}
						else
						{
							defaulters.put(wid, 1);
						}
					}
				}
			}
		}
		catch(Exception e)
		{
			e.printStackTrace();
		}
		
		if(defaulters.size()==0)
		{
			//no defaulters
			System.out.println("No defaulters as of now!!");
		}
		else
		{
			System.out.println("--------------------DEFAULTER LIST-------------------");
			for(Map.Entry m:defaulters.entrySet())
			{
				q = "select * from worker where worker_id='"+m.getKey()+"';";
				rs = ob.selectData(q);
				try
				{
					while(rs.next())
					{
						System.out.println("Name: "+rs.getString("worker_fname")+" "+rs.getString("worker_lname"));
						System.out.println("Worker Id: "+rs.getString("worker_id"));
						System.out.println("Worker Skill:" +rs.getString("worker_skill"));
						System.out.println("No of task not completed : "+m.getValue());
						System.out.println("------------------------------------------------");
					}
				}
				catch(Exception e)
				{
					
					
				}
			}
		}
		
		
	}
	
}
