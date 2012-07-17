package adonai.metaweblog_client;
 
import java.net.MalformedURLException;
import java.net.URL;
import java.util.Map;

import de.timroes.axmlrpc.XMLRPCClient;
import de.timroes.axmlrpc.XMLRPCException;

public class JMetaWeblogClient {
	private URL serviceURL;
	private String postID;
	private String username;
	private String password;
	private String blogId;
	private Map <String,String> struts;
	private XMLRPCClient client;
	
	public JMetaWeblogClient()
	{ 
	}
	
	public JMetaWeblogClient(String servURL) throws MalformedURLException
	{
		this.client = new XMLRPCClient(new URL(servURL));			
	}
	
	public String newPost() throws XMLRPCException
	{
		Object[] params = new Object[]{blogId, this.username, this.password, this.struts, true};  
		  
		String ret = (String) this.client.call("metaWeblog.newPost", params);  
		return ret;
	}
	
	public String newPost(Map<String,String> m) throws XMLRPCException
	{
		Object[] params = new Object[]{blogId, this.username, this.password, m, true};  
		  
		String ret = (String) this.client.call("metaWeblog.newPost", params);  
		return ret;
	}
	
	public boolean editPost() throws XMLRPCException
	{
		Object[] params = new Object[]{this.postID, this.username, this.password, this.struts, true};
		boolean ret = (Boolean) this.client.call("metaWeblog.editPost", params);
		return ret;
	}
	
	public boolean editPost(String Id) throws XMLRPCException
	{
		Object[] params = new Object[]{Id, this.username, this.password, this.struts, true};
		boolean ret = (Boolean) this.client.call("metaWeblog.editPost", params);
		return ret;
	}
	
	
	public boolean editPost(String postId,Map<String,String> m) throws XMLRPCException
	{
		Object[] params = new Object[]{postId, this.username, this.password, m, true};
		boolean ret = (Boolean) this.client.call("metaWeblog.editPost", params);
		return ret;
	}
	
	public boolean editPost(String Id,String uname,String pword,Map<String,String> m) throws XMLRPCException
	{
		Object[] params = new Object[]{Id, uname, pword, m, true};
		boolean ret = (Boolean) this.client.call("metaWeblog.editPost", params);
		return ret;
	}
	
	@SuppressWarnings("unchecked")
	public Map<String,String> getPost(String Id) throws XMLRPCException
	{
		Object[] params = new Object[]{Id,this.username,this.password};
		return (Map<String, String>) this.client.call("metaWeblog.getPost", params);		
	}
	@SuppressWarnings("unchecked")
	public Map<String,String> getPost(String Id,String uname, String pword) throws XMLRPCException
	{
		Object[] params = new Object[]{Id,uname,pword};
		return (Map<String, String>) this.client.call("metaWeblog.getPost", params);		
	}
	

	public Object[] getRecentPosts (int numberOfPost) throws XMLRPCException
	{
		Object[] params = new Object[]{blogId,this.username,this.password,numberOfPost};
		return (Object[]) this.client.call("metaWeblog.getRecentPosts", params);		
	}
	
	public Object[] getUsersBlogs () throws XMLRPCException
	{
		Object[] params = new Object[]{"",this.username,this.password};
		return (Object[]) this.client.call("blogger.getUsersBlogs", params);		
	}
	
	public Object[] getRecentPosts (String uname, String pword,int numberOfPost) throws XMLRPCException
	{
		Object[] params = new Object[]{blogId,uname,pword,numberOfPost};
		return (Object[]) this.client.call("metaWeblog.getRecentPosts", params);		
	}
	
	public Object[] getCategories() throws XMLRPCException
	{
		Object[] params = new Object[]{blogId,this.username,this.password};
		return (Object[]) this.client.call("metaWeblog.getCategories", params);	
	}
	
//	@SuppressWarnings("unchecked")
//	public boolean deletePost(String Id) throws XmlRpcException
//	{
//		Object[] params = new Object[]{Id,this.username,this.password};
//		return (Boolean) this.client.call("metaWeblog.deletePost", params);		
//	}
//	@SuppressWarnings("unchecked")
//	public boolean deletePost(String Id,String uname, String pword) throws XmlRpcException
//	{
//		Object[] params = new Object[]{Id,uname,pword};
//		return (Boolean) this.client.call("metaWeblog.deletePost", params);		
//	}
	
	public URL getServiceURL() {
		return serviceURL;
	}
	public void setServiceURL(URL serviceURL) {
		this.serviceURL = serviceURL;
	}
	public String getBlogId() {
		return blogId;
	}
	public void setBlogId(String bId) {
		this.blogId = bId;
	}
	public String getUsername() {
		return username;
	}
	public void setUsername(String username) {
		this.username = username;
	}
	public String getPassword() {
		return password;
	}
	public void setPassword(String password) {
		this.password = password;
	}
	public Map<String, String> getStruts() {
		return struts;
	}
	public void setStruts(Map<String, String> struts) {
		this.struts = struts;
	}
	public XMLRPCClient getClient() {
		return client;
	}
	public void setClient(XMLRPCClient client) {
		this.client = client;
	}

	public String getPostID() {
		return postID;
	}

	public void setPostID(String postID) {
		this.postID = postID;
	}
	

}
