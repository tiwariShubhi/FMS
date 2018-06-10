package com.oopd.fms;

import java.sql.Date;
import java.sql.ResultSet;
import java.sql.Time;
import java.util.ArrayList;
import java.util.List;

import com.oopd.dao.DBService;
import com.oopd.dao.DaoLayer;
import com.restfb.Connection;
import com.restfb.DefaultFacebookClient;
import com.restfb.FacebookClient;
import com.restfb.Parameter;
import com.restfb.types.Comment;
import com.restfb.types.FacebookType;
import com.restfb.types.Group;
import com.restfb.types.Post;

public class FbClient {

	private String accToken;
	private String groupId;
	FacebookClient fbc;
	ArrayList<post> newFeed ;
	DaoLayer ob ;
	private String fmsTeamId;
	
	FbClient()
	{
		setAccToken("EAAV447NiKnEBAF6TZAgT09XA1zSBM4Yk0YzCXiDbapeZBifPw5XQab5ZCZCPiO0FZBZBJU2mP74CYVZCY0AvucjyKJtMDV2uUmFWf6pbmr6LGw6MBG1JwG3MhDqcPyra2nfBDpBnAoYkMVOZAg3CSIjEBpNYIl6txeq9bXqRiRyoCobYTwPn4YCm");
		setGroupId("1900646103532951");
		newFeed = new ArrayList<post>(); 
		ob = new DBService();
		fmsTeamId = "133804520666316";
	}
	
	public void connect()
	{
		fbc = new DefaultFacebookClient(accToken);
		
	}

	public String getAccToken() {
		return accToken;
	}

	public void setAccToken(String accToken) {
		this.accToken = accToken;
	}
	
	public String getGroupId() {
		return groupId;
	}

	public void setGroupId(String groupId) {
		this.groupId = groupId;
	}
	
	
	public void readFeed()
	{
		Group grp = fbc.fetchObject(groupId, Group.class);
		
		DaoLayer ob = new DBService();
		
		Connection<Post> postFeed = fbc.fetchConnection(grp.getId()+"/feed", Post.class,Parameter.with("fields", "id,message,name,from,comments,created_time"));
		for(List<Post> postPage : postFeed)
		{
			for(Post aPost : postPage)
			{
				String pid = aPost.getId();
				String q = "select * from post where post_id ='"+pid+"';";	
				System.out.println(q);
				ResultSet rs = ob.selectData(q);
				boolean empty = true;
				
					System.out.println("in here");
					//post is not new...
					//check if it is allotment post or feedback post
				try
				{
						//rs.next();
						while(rs.next())
						{
							empty=false;
							String pType = rs.getString("post_type");
							String pStatus = rs.getString("status");
							String compId = rs.getString("post_type_id");
							if(pType.equals("A") && pStatus.equals("ON") )
							{
								//we are looking for a comment with work done by worker and 
								//we will initiate feedback from here
								workDone(aPost,pid,compId);
								
							}
							else if(pType.equals("F") && pStatus.equals("ON"))
							{
								// we will look for feedback from user top 1 comment and store it as feedback
								processFeedback(aPost, compId, pid);
							}
							else
							{
								continue;
							}
						}	
				}
				catch(Exception e)
				{
					e.printStackTrace();
				}
					
					
				
				if(empty)
				{
					//post is new
					//no need to store posts created by FMS Team
					
					
					System.out.println("new post");
					
					if(aPost.getFrom().getId().equals(fmsTeamId))
					{
						//skip these posts
					}
					else
					{
						System.out.println("new post");
						String timeStamp = aPost.getCreatedTime().toString();
						
						Date postDate = formatDate(timeStamp);
						Time postTime = formatTime(timeStamp);
						
						post p = new post();
						p.setCreator(aPost.getFrom().getName());
						p.setCreatorId(aPost.getFrom().getId());
						p.setId(aPost.getId());
						p.setMsg(aPost.getMessage());
						p.setCreatedDate(postDate);
						p.setCreatedTime(postTime);
						newFeed.add(p);
						
					}
				}

			}
		}
	
		
		
		
		
				
		
		
	}
	
	void workDone(Post aPost,String pid,String compId)
	{
		String wId = "";
		String sId="";
		//check if work done 
		if(aPost.getComments()!=null)
		{
			List<Comment> com = aPost.getComments().getData();
			System.out.println("------------Comments--**------------------");
			//System.out.println("com not null :");
			for(Comment c : com)
			{
				String commsg = c.getMessage().toLowerCase();
				String posterId = c.getFrom().getId();
				String q = "select worker_id,student_id from complaint where comp_id="+compId+";";
				System.out.println(q);
				ResultSet rs = ob.selectData(q);
				boolean empty = true;
				
					try
					{
						while(rs.next())
						{
							empty=false;
						wId = rs.getString(1);
						sId  = rs.getString(2);
						}
					}
					catch(Exception e)
					{
						e.printStackTrace();
					}
				
				q= "select worker_fb_id from worker where worker_id='"+wId+"';";	
				
				System.out.println(q);
				rs = ob.selectData(q);
				String wFbId="";
				empty = true;
				
					try
					{
						while(rs.next())
						{
							empty=false;
						wFbId = rs.getString(1);
						
						}
					}
					catch(Exception e)
					{
						e.printStackTrace();
					}
				
				
					
				
				if(wFbId.equals(posterId))
				{
					//it is a comment by the worker
					if(commsg.equalsIgnoreCase("work done"))
					{
						//work is done
						//highlight changes in database
						q = "update post set status='OFF' where post_id='"+pid+"';";
						System.out.println(q);
						int k = ob.updateData(q);
						String timeStamp = aPost.getCreatedTime().toString();
						
						Date postDate = formatDate(timeStamp);
						Time postTime = formatTime(timeStamp);
						
						q = "update complaint set comp_status='CLOSED', comp_finish_date='"+postDate+"',comp_finish_time='"+postTime+"' where comp_id='"+compId+"';";
						System.out.println(q);
						 k = ob.updateData(q);
						
						 //update worker table
						 q="update worker set worker_status='A',no_task_comp=no_task_comp+1;";
						 System.out.println(q);
						 k = ob.updateData(q);
						 
						 
						//create a feedback post for 
						 //find workerFbId and send student fb Id
						 //send compId and sId too
						 String sFbId="";
						 q="select student_fb_id from student where student_id='"+sId+"';";
						 
						 rs = ob.selectData(q);
						 
							try
							{
								while(rs.next())
									sFbId = rs.getString(1);
							}
							catch(Exception e)
							{
								e.printStackTrace();
							}
						
						 
						 q="select worker_fb_id from worker where worker_id='"+wId+"';";
						 
						 rs = ob.selectData(q);
						 
							try
							{
								while(rs.next())
									wFbId = rs.getString(1);
							}
							catch(Exception e)
							{
								e.printStackTrace();
							}
						
						 
						 initiateFeedback(aPost, wFbId, compId, sFbId, sId);
					}
				}
				
			}
		}
		else
		{
			//worker is yet to finish work...
			//do nothing
		}
	}
	
	void initiateFeedback(Post aPost,String wFbId,String compId,String sFbId,String sId)
	{
		
		//create db entry in feedback table
		String q="select max(feedback_id) from feedback;";
		ResultSet rs = ob.selectData(q);
		String feeId="";
		
			
			try
			{
				while(rs.next())
					feeId = rs.getString(1);
				
			}
			catch(Exception e)
			{
				e.printStackTrace();
			}
			
			
		
		
		q = "insert into feedback (feedback_msg,feedback_date,feedback_time,comp_id,student_id) values('',null,null,'"+compId+"','"+sId+"');";
		System.out.println(q);
		int k = ob.updateData(q);
		
		
		
		
		//create the post on fb
		String postMsg = "Hurray! Your complain has been addressed. \nPlease provide your valuable feedback regarding complaint No. " +compId+" for @["+wFbId+"] and help us server you better. :) ";
		FacebookType resp1 = fbc.publish(groupId+"/feed",FacebookType.class,Parameter.with("message", postMsg),Parameter.with("tags", ""+sFbId+","+wFbId));
		System.out.println(resp1.getId());
		
		//create entry in post table
		feeId = (Integer.parseInt(feeId)+1) + "";
		
		q = "insert into post values('"+resp1.getId()+"','F','"+feeId+"','ON');";
		System.out.println(q); 
		k = ob.updateData(q);
		
	}
	
	void processFeedback(Post aPost,String compId,String pId)
	{

		String wId = "";
		String sId="";
		if(aPost.getComments()!=null)
		{
			List<Comment> com = aPost.getComments().getData();
			System.out.println("------------Comments on Feedback--**------------------");
			//System.out.println("com not null :");
			for(Comment c : com)
			{
				String commsg = c.getMessage().toLowerCase();
				String posterId = c.getFrom().getId();
				String q = "select worker_id,student_id from complaint where comp_id="+compId+";";
				System.out.println(q);
				ResultSet rs = ob.selectData(q);
				
					try
					{
						while(rs.next())
						{
						wId = rs.getString(1);
						sId  = rs.getString(2);
						}
					}
					catch(Exception e)
					{
						e.printStackTrace();
					}
				
				
				if(sId.equals(posterId))
				{
					//student is the one...
					// put this feedback in db
					String timeStamp = aPost.getCreatedTime().toString();
					
					Date postDate = formatDate(timeStamp);
					Time postTime = formatTime(timeStamp);
					q = "update feedback set feedback_msg='"+commsg+"',feedback_date='"+postDate+"',feedback_time='"+postTime+"' where comp_id='"+compId+"';";
					System.out.println(q);
					int k = ob.updateData(q);
					
					//update post table
					q = "update post set status='OFF where post_id='"+pId+"';";
					System.out.println(q);
					k = ob.updateData(q);
					//post a thanku for feedback
					FacebookType resp1 = fbc.publish(pId+"/comments",FacebookType.class,Parameter.with("message", "Thankyou for your Feedback :) !"));
					System.out.println(resp1.getId());
					
				}
			}
		}
	}
	
	void parsePost()
	{
		String []pMsg;
		String q="";
		
		for(post p:newFeed)
		{
			if(p.getMsg()!=null)
			{
				
				pMsg = p.getMsg().split(":");
				
				if(pMsg[0].equalsIgnoreCase("Register")&& pMsg.length==6)
				{
					if(pMsg[1].equalsIgnoreCase("Student"))
					{
						int count = checkStudentRegistered(p.getCreatorId());
						if(count>0)
						{
							//error user already exists
							FacebookType resp1 = fbc.publish(p.getId()+"/comments",FacebookType.class,Parameter.with("message", "Sorry, but you are already registered with us.Keep calm and complain! :) "));
							System.out.println(resp1.getId());
						}
						else
						{
							//check if student registered as worker
							count = checkWorkerRegistered(p.getCreatorId());
							if(count>0)
							{
								//studetn trying to register as worker
								FacebookType resp1 = fbc.publish(p.getId()+"/comments",FacebookType.class,Parameter.with("message", "Sorry, but you are already registered with us as a Student, can't register you as a worker. :( "));
								System.out.println(resp1.getId());
							}
							else
							{
								//new user
								q = "insert into student values('" + pMsg[4].toLowerCase() +"','" + pMsg[2].toLowerCase() +"','"+ pMsg[3].toLowerCase()+"','"+pMsg[5].toLowerCase()+"','"+p.getCreatorId()+"')";
								System.out.println(q);
								int ret = ob.updateData(q);
								FacebookType resp1 = fbc.publish(p.getId()+"/comments",FacebookType.class,Parameter.with("message", "Congratulations! You are now registered with FMS Team!"));
								System.out.println(resp1.getId());
								// save this post in post table
								
								q = "insert into post values('"+resp1.getId()+"','R','"+pMsg[4].toLowerCase()+"',status='OFF');";
								System.out.println(q);
								ret = ob.updateData(q);
								
							}
							
						}
					}
					else if(pMsg[1].equalsIgnoreCase("Worker") && pMsg.length==6)
					{
						int count = checkWorkerRegistered(p.getCreatorId());
						
						if(count>0)
						{
							//error user already exists
							FacebookType resp1 = fbc.publish(p.getId()+"/comments",FacebookType.class,Parameter.with("message", "Sorry, but you are already registered with us.Keep calm and wait for work! :) "));
							System.out.println(resp1.getId());
						}
						else
						{
							count = checkStudentRegistered(p.getCreatorId());
							
							if(count>0)
							{
								//studetn trying to register as worker
								FacebookType resp1 = fbc.publish(p.getId()+"/comments",FacebookType.class,Parameter.with("message", "Sorry, but you are already registered with us as a Worker, can't register you as a Student. :( "));
								System.out.println(resp1.getId());
							}
							else
							{
								
								q = "insert into worker values('" + pMsg[4].toLowerCase() +"','" + pMsg[2].toLowerCase() +"','"+ pMsg[3].toLowerCase()+"','"+pMsg[5].toLowerCase()+"','"+p.getCreatorId()+"',0,0,'A')";
								//status A = Available
								// U = unavailable
								System.out.println(q);
								int ret = ob.updateData(q);
								FacebookType resp1 = fbc.publish(p.getId()+"/comments",FacebookType.class,Parameter.with("message", "Congratulations! You are now registered with FMS Team!"));
								System.out.println(resp1.getId());
								
								q = "insert into post values('"+resp1.getId()+"','R','"+pMsg[4].toLowerCase()+"',status='OFF');";
								System.out.println(q);
								ret = ob.updateData(q);
							}
							
							
						}
						
						
						
					}
				}
				else if(pMsg[0].equalsIgnoreCase("Complaint"))
				{
					String compId="0";
					//check it is legitiamte , registered student..
					int count = checkStudentRegistered(p.getCreatorId());
					if(count==1)
					{
						//parse complaint string
						
						if(pMsg.length==4)
						{
							String category =pMsg[1].toLowerCase();
							String location =pMsg[2].toLowerCase();
							String comMsg = pMsg[3].toLowerCase();
							String wId = "";
							String wFbId="";
							int noTaskAssg=0;
							
							//check if worker available
							
							q="select worker_id,no_task_assg,worker_fb_id from worker where worker_status = 'A' and worker_skill='"+category+"';";
							System.out.println(q);
							ResultSet rs = ob.selectData(q);
							
							try
							{
								while(rs.next())
								{
									//pick first available worker...
									wId=rs.getString(1);
									noTaskAssg = rs.getInt(2);
									wFbId = rs.getString(3);
									break;
								}
							}
							catch(Exception e)
							{
								e.printStackTrace();
							}
							
							if(wId=="")
							{
								//no worker available...
								//post no worker available
								System.out.println("worker id blank");
								FacebookType resp1 = fbc.publish(p.getId()+"/comments",FacebookType.class,Parameter.with("message", "Sorry, no worker is available at the moment.Please try after sometime"));
								System.out.println(resp1.getId());
							}
							else
							{
								//create complaint entry in db
								String sId="";
								q="Select student_id from student where student_fb_id='"+p.getCreatorId()+"';";
								System.out.println(q);
								ResultSet rs1 = ob.selectData(q);
								try
								{
									
										while(rs1.next())
										{
											sId = rs1.getString(1);
										}
									
								}
								catch(Exception e)
								{
									e.printStackTrace();
								}
								
								q="select max(comp_id) from complaint";
								rs1 = ob.selectData(q);
								
								
								try
								{
										while(rs1.next())
										{
											compId = rs1.getString(1);
											System.out.println(compId);
										}
									
								}
								catch(Exception e)
								{
									e.printStackTrace();
								}
								
								//if(compId==null)
									//{compId="0"; System.out.println("Done"); }
								
								
								System.out.println("compId : "+compId);
								int x = Integer.parseInt(compId);
								x++;
								
								compId = x+"";
								
								
								q="insert into complaint(comp_id,comp_category,comp_msg,student_id,worker_id,comp_status,comp_log_date,comp_log_time,comp_finish_date,comp_finish_time,comp_location) "
										+ "values("+compId+",'"+category+"','"+comMsg+"','"+sId+"','"+wId+"','Open','"+p.getCreatedDate()+"','"+p.getCreatedTime()+"',null,null,'"+location+"');";
								System.out.println(q);
								int k = ob.updateData(q);
								System.out.println("inserted into db : complaint");
								
								//then reply him that work will be done shortly
								FacebookType resp1 = fbc.publish(p.getId()+"/comments",FacebookType.class,Parameter.with("message", "We have recorded your complaint wit complaint Id : "+compId+" . A professional will be assigned shortly"));
								System.out.println(resp1.getId());
								
								//create a new post with worker assigned to the work.. include previous post summary..
								//tag student and worker
								System.out.println(p.getCreatorId()+" "+wFbId);
								
								String postMsg="Complaint : "+comMsg+"\n Complaint Id : "+compId+"\n Assigned to : @["+wFbId+"]";
								resp1 = fbc.publish(groupId+"/feed",FacebookType.class,Parameter.with("message", postMsg),Parameter.with("tags", ""+p.getCreatorId()+","+wFbId));
								System.out.println(resp1.getId());

								//put this info in database
								//worker curent task table and worker task..
								
								noTaskAssg++;
								q="update worker set no_task_assg="+noTaskAssg+",worker_status='U' where worker_id='"+wId+"';";
								System.out.println(q);
								k = ob.updateData(q);
								System.out.println("inserted into db : worker no updated");
								
								q = "insert into post values('"+resp1.getId()+"','A','"+compId+"','ON');";
								System.out.println(q);
								k = ob.updateData(q);
								
								
								
							}
						
							
						 
						
						
						//put this info in database
						}
						else
						{
							FacebookType resp1 = fbc.publish(p.getId()+"/comments",FacebookType.class,Parameter.with("message", "You have posted an invalid query.Please refer to the pinned post to see Complaint/Feedback format."));
							System.out.println(resp1.getId());
						}
						//original user post	
						q = "insert into post values('"+p.getId()+"','A','"+compId+"','OFF');";
						System.out.println(q);
						int k = ob.updateData(q);	
							
							
					}
					else 
					{
						///student not registered yet...
						FacebookType resp1 = fbc.publish(p.getId()+"/comments",FacebookType.class,Parameter.with("message", ":( Seems like you have not registered with us yet.Please Register inorder to file a complaint."));
						System.out.println(resp1.getId());
					}	
					
				}
				else
				{
					//Invalid command
					// post a comment to the post saying invalid command string..
					FacebookType resp1 = fbc.publish(p.getId()+"/comments",FacebookType.class,Parameter.with("message", "You have posted an invalid query.Please refer to the pinned post to see Complaint/Feedback format."));
					System.out.println(resp1.getId());
					//original user post	
					q = "insert into post values('"+p.getId()+"','I','','OFF');";
					System.out.println(q);
					int k = ob.updateData(q);
				}
				
			}
		}
		
	}
	
	
	
	Date formatDate(String timeStamp) 
	{
		// parse the String only createdTimeStamp to java.time.LocalDate and time
		
		String [] a = timeStamp.split("\\s");
		//System.out.println(timeStamp);
		String mon= a[1];
		int day = Integer.parseInt(a[2]);
		int year = Integer.parseInt(a[5]);
		int month = findMonth(mon);
		
		//System.out.println(day+" "+month+" "+" "+year);
		return new java.sql.Date(year-1900, month-1, day);
		
	}
	
	

	Time formatTime(String timeStamp) 
	{
		// parse the String only createdTimeStamp to java.time.LocalDate and time
		
		String [] a = timeStamp.split("\\s");
		String [] time = a[3].split(":");
		
		
		
		
		return new java.sql.Time(Integer.parseInt(time[0]),Integer.parseInt(time[1]),Integer.parseInt(time[2]));
		
	}
	
	
	

	int findMonth(String mon)
	{
		int m=0;
		
		if(mon.equalsIgnoreCase("JAN"))
		{
			m=1;
		}
		else if(mon.equalsIgnoreCase("FEB"))
		{
			m=2;
		}
		else if(mon.equalsIgnoreCase("MAR"))
		{
			m=3;
		}
		else if(mon.equalsIgnoreCase("APR"))
		{
			m=4;
		}
		else if(mon.equalsIgnoreCase("MAY"))
		{
			m=5;
		}
		else if(mon.equalsIgnoreCase("JUN"))
		{
			m=6;
		}
		else if(mon.equalsIgnoreCase("JUL"))
		{
			m=7;
		}
		else if(mon.equalsIgnoreCase("AUG"))
		{
			m=8;
		}
		else if(mon.equalsIgnoreCase("SEP"))
		{
			m=9;
		}
		else if(mon.equalsIgnoreCase("OCT"))
		{
			m=10;
		}
		else if(mon.equalsIgnoreCase("NOV"))
		{
			m=11;
		}
		else if(mon.equalsIgnoreCase("DEC"))
		{
			m=12;
		}
		else
		{
			m=0;
		}
		
		return m;
	}
	

	int checkStudentRegistered(String id)
	{
		
		int count=0;
		String q="Select count(student_id) from student where student_fb_id='"+id+"';";
		ResultSet rs = ob.selectData(q);
		try
		{
			while(rs.next())
			{
				count = rs.getInt(1);
			}
		}
		catch(Exception e)
		{
			e.printStackTrace();
		}
		
		return count;
	}
	
	int checkWorkerRegistered(String id)
	{
		int count=0;
		String q="Select count(worker_id) from worker where worker_fb_id='"+id+"';";
		ResultSet rs = ob.selectData(q);
		try
		{
			while(rs.next())
			{
				count = rs.getInt(1);
			}
		}
		catch(Exception e)
		{
			e.printStackTrace();
		}
		return count;
	}
	
}
