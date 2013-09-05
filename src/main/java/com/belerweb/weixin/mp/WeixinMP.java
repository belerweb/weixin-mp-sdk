package com.belerweb.weixin.mp;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.commons.codec.digest.DigestUtils;
import org.apache.commons.httpclient.HttpClient;
import org.apache.commons.httpclient.HttpException;
import org.apache.commons.httpclient.HttpMethod;
import org.apache.commons.httpclient.HttpStatus;
import org.apache.commons.httpclient.NameValuePair;
import org.apache.commons.httpclient.cookie.CookiePolicy;
import org.apache.commons.httpclient.methods.GetMethod;
import org.apache.commons.httpclient.methods.PostMethod;
import org.apache.commons.httpclient.methods.multipart.MultipartRequestEntity;
import org.apache.commons.httpclient.methods.multipart.Part;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.jsoup.helper.StringUtil;

/**
 * 微信公众平台
 */
public class WeixinMP {

  public static final String CONFIG_USERNAME = "weixin.mp.username";
  public static final String CONFIG_PASSWORD = "weixin.mp.password";

  public static final int GROUP_DEFAULT = 0;// 未分组
  public static final int GROUP_BLACKLIST = 1;// 黑名单
  public static final int GROUP_ASTERISK = 2;// 星标组

  public static final String IMAGE_JPG = "image/jpeg";
  public static final String IMAGE_PNG = "image/png";
  public static final String IMAGE_GIF = "image/gif";

  private static final Map<String, WeixinMP> MP = new HashMap<String, WeixinMP>();

  private HttpClient httpClient;
  private String username;
  private String password;
  private String token;
  private long tokenTime;

  /**
   * 私有构造函数，请通过init方法获取实例
   */
  private WeixinMP(String username, String password) {
    this.username = username;
    this.password = password;
    httpClient = new HttpClient();
    httpClient.getParams().setCookiePolicy(CookiePolicy.BROWSER_COMPATIBILITY);
    httpClient.getParams().setParameter("http.protocol.single-cookie-header", true);
  }

  /**
   * 获取凭证
   */
  public AccessToken getAccessToken(String appid, String secret) throws MpException {
    return getAccessToken("client_credential", appid, secret);
  }

  /**
   * 获取凭证
   */
  public AccessToken getAccessToken(String grantType, String appid, String secret)
      throws MpException {
    GetMethod request = new GetMethod("https://api.weixin.qq.com/cgi-bin/token");
    NameValuePair[] params = new NameValuePair[3];
    params[0] = new NameValuePair("grant_type", grantType);
    params[1] = new NameValuePair("appid", appid);
    params[2] = new NameValuePair("secret", secret);
    request.setQueryString(params);
    return new AccessToken(toJsonObject(execute(request)));
  }

  /**
   * 微信公众平台初始化
   */
  public static WeixinMP init(String username, String password) throws MpException {
    if (!MP.containsKey(username)) {
      WeixinMP mp = new WeixinMP(username, password);
      mp.login();
      MP.put(username, mp);
    }

    return MP.get(username);
  }

  /**
   * 检查Token是否过期
   */
  private void checkToken() throws MpException {
    if (token == null || (System.currentTimeMillis() - tokenTime) > 600000) {
      login();
    }
  }

  /**
   * 登录
   */
  private void login() throws MpException {
    PostMethod request = new PostMethod("https://mp.weixin.qq.com/cgi-bin/login");
    request.addParameter("X-Requested-With", "XMLHttpRequest");
    request.addParameter(new NameValuePair("lang", "zh_CN"));
    request.addParameter(new NameValuePair("f", "json"));
    request.addParameter(new NameValuePair("imgcode", ""));
    request.addParameter(new NameValuePair("username", username));
    request.addParameter(new NameValuePair("pwd", DigestUtils.md5Hex(password)));

    /**
     * 正确结果示例:
     * 
     * { "Ret": 302, "ErrMsg": "/cgi-bin/home?t=home/index&lang=zh_CN&token=1234567890",
     * "ShowVerifyCode": 0, "ErrCode": 0 }
     * 
     * 错误结果示例:
     * 
     * { "Ret": 400, "ErrMsg": "", "ShowVerifyCode": 0, "ErrCode": -3 } // 密码错误
     * 
     * { "Ret": 400, "ErrMsg": "", "ShowVerifyCode": 0, "ErrCode": -2 } // HTTP错误
     */
    try {
      JSONObject result = new JSONObject(execute(request));
      if (result.getInt("Ret") == 302) {
        Matcher matcher = Pattern.compile("token=(\\d+)").matcher(result.getString("ErrMsg"));
        if (matcher.find()) {
          token = matcher.group(1);
          tokenTime = new Date().getTime();
        }
      }
    } catch (JSONException e) {
      throw new MpException(e);
    }
  }

  /**
   * 实时消息：全部消息
   */
  public List<WeixinMessage> getMessage(int offset, int count) throws MpException {
    checkToken();
    String url = "https://mp.weixin.qq.com/cgi-bin/message";
    GetMethod request = new GetMethod(url);
    NameValuePair[] params = new NameValuePair[5];
    params[0] = new NameValuePair("token", token);// 必须
    params[1] = new NameValuePair("lang", "zh_CN");// 必须
    params[2] = new NameValuePair("day", "7");// 全部消息
    params[3] = new NameValuePair("offset", String.valueOf(offset));
    params[4] = new NameValuePair("count", String.valueOf(count));
    request.setQueryString(params);
    List<WeixinMessage> messages = new ArrayList<WeixinMessage>();
    for (String line : execute(request).split("[\r\n]+")) {
      line = line.trim();
      if (line.startsWith("list : ({\"msg_item\":") && line.endsWith("}).msg_item")) {
        try {
          JSONArray array = new JSONArray(line.substring(20, line.length() - 11));
          for (int i = 0; i < array.length(); i++) {
            messages.add(new WeixinMessage(array.getJSONObject(i)));
          }
        } catch (JSONException e) {
          throw new MpException(e);
        }
        break;
      }
    }
    return messages;
  }

  /**
   * 用户列表
   */
  public List<WeixinUser> getUser(int groupId, int pageidx, int pagesize) throws MpException {
    checkToken();
    String url = "https://mp.weixin.qq.com/cgi-bin/contactmanage";
    GetMethod request = new GetMethod(url);
    NameValuePair[] params = new NameValuePair[6];
    params[0] = new NameValuePair("token", token);
    params[1] = new NameValuePair("lang", "zh_CN");
    params[2] = new NameValuePair("type", "0");
    params[3] = new NameValuePair("groupid", String.valueOf(groupId));
    params[4] = new NameValuePair("pageidx", String.valueOf(pageidx));
    params[5] = new NameValuePair("pagesize", String.valueOf(pagesize));
    request.setQueryString(params);
    List<WeixinUser> users = new ArrayList<WeixinUser>();
    for (String line : execute(request).split("[\r\n]+")) {
      line = line.trim();
      if (line.startsWith("friendsList : ({\"contacts\":") && line.endsWith("}).contacts,")) {
        try {
          JSONArray array = new JSONArray(line.substring(27, line.length() - 12));
          for (int i = 0; i < array.length(); i++) {
            users.add(new WeixinUser(array.getJSONObject(i)));
          }
        } catch (JSONException e) {
          throw new MpException(e);
        }
        break;
      }
    }
    return users;
  }

  /**
   * 通过FakeId获取指定用户信息
   */
  public WeixinUser getUser(String fakeId) throws MpException {
    checkToken();
    String url = "https://mp.weixin.qq.com/cgi-bin/getcontactinfo";
    PostMethod request = new PostMethod(url);
    request.addParameter("token", token);
    request.addParameter("lang", "zh_CN");
    request.addParameter("t", "ajax-getcontactinfo");
    request.addParameter("fakeid", fakeId);
    return new WeixinUser(toJsonObject(execute(request)));
  }

  /**
   * 将用户放入某个组内
   */
  public boolean putIntoGroup(List<String> fakeIds, int groupId) throws MpException {
    checkToken();
    String url = "https://mp.weixin.qq.com/cgi-bin/modifycontacts";
    PostMethod request = new PostMethod(url);
    request.addParameter("token", token);
    request.addParameter("lang", "zh_CN");
    request.addParameter("t", "ajax-putinto-group");
    request.addParameter("action", "modifycontacts");
    request.addParameter("tofakeidlist", StringUtil.join(fakeIds, "|"));
    request.addParameter("contacttype", String.valueOf(groupId));
    return "0".equals(toJsonObject(execute(request)).optString("ret"));
  }

  /**
   * 增加分组
   */
  public boolean addGroup(String name) throws MpException {
    checkToken();
    String url = "https://mp.weixin.qq.com/cgi-bin/modifygroup";
    PostMethod request = new PostMethod(url);
    request.addParameter("token", token);
    request.addParameter("lang", "zh_CN");
    request.addParameter("t", "ajax-friend-group");
    request.addParameter("func", "add");
    request.addParameter("name", name);
    return toJsonObject(execute(request)).optString("GroupId", null) != null;
  }

  /**
   * 修改组名
   */
  public boolean renameGroup(int groupId, String name) throws MpException {
    checkToken();
    String url = "https://mp.weixin.qq.com/cgi-bin/modifygroup";
    PostMethod request = new PostMethod(url);
    request.addParameter("token", token);
    request.addParameter("lang", "zh_CN");
    request.addParameter("t", "ajax-friend-group");
    request.addParameter("func", "rename");
    request.addParameter("id", String.valueOf(groupId));
    request.addParameter("name", name);
    return toJsonObject(execute(request)).optString("GroupId", null) != null;
  }

  /**
   * 删除分组
   */
  public boolean deleteGroup(int groupId) throws MpException {
    checkToken();
    String url = "https://mp.weixin.qq.com/cgi-bin/modifygroup";
    PostMethod request = new PostMethod(url);
    request.addParameter("token", token);
    request.addParameter("lang", "zh_CN");
    request.addParameter("t", "ajax-friend-group");
    request.addParameter("func", "del");
    request.addParameter("id", String.valueOf(groupId));
    return toJsonObject(execute(request)).optString("GroupId", null) != null;
  }

  /**
   * 发送文字消息
   */
  public boolean sendText(String fakeId, String content) throws MpException {
    checkToken();
    String url = "https://mp.weixin.qq.com/cgi-bin/singlesend";
    PostMethod request = new PostMethod(url);
    request.addRequestHeader("Referer", "https://mp.weixin.qq.com/cgi-bin/singlemsgpage");
    request.addParameter("token", token);
    request.addParameter("lang", "zh_CN");
    request.addParameter("t", "ajax-response");
    request.addParameter("error", "false");
    request.addParameter("imgcode", "");
    request.addParameter("ajax", "1");
    request.addParameter("type", "1");// 文字
    request.addParameter("tofakeid", fakeId);
    request.addParameter("content", content);
    return toJsonObject(execute(request)).optInt("ret", -1) == 0;
  }

  /**
   * 发送图片消息
   */
  public boolean sendImage(String fakeId, String type, byte[] imageData) throws MpException {
    checkToken();
    int fileid = uploadImage(type, imageData);
    String url = "https://mp.weixin.qq.com/cgi-bin/singlesend";
    PostMethod request = new PostMethod(url);
    request.addRequestHeader("Referer", "https://mp.weixin.qq.com/cgi-bin/singlemsgpage");
    request.addParameter("token", token);
    request.addParameter("lang", "zh_CN");
    request.addParameter("t", "ajax-response");
    request.addParameter("error", "false");
    request.addParameter("imgcode", "");
    request.addParameter("ajax", "1");
    request.addParameter("type", "2");// 图片
    request.addParameter("tofakeid", fakeId);
    request.addParameter("fid", String.valueOf(fileid));
    request.addParameter("fileid", String.valueOf(fileid));
    boolean result = toJsonObject(execute(request)).optInt("ret", -1) == 0;
    deleteFile(fileid);
    return result;
  }

  public boolean sendImageText(String fakeId, int appMsgId) throws MpException {
    checkToken();
    String url = "https://mp.weixin.qq.com/cgi-bin/singlesend";
    PostMethod request = new PostMethod(url);
    request.addRequestHeader("Referer", "https://mp.weixin.qq.com/cgi-bin/singlemsgpage");
    request.addParameter("token", token);
    request.addParameter("lang", "zh_CN");
    request.addParameter("t", "ajax-response");
    request.addParameter("error", "false");
    request.addParameter("imgcode", "");
    request.addParameter("ajax", "1");
    request.addParameter("type", "10");// 图文
    request.addParameter("tofakeid", fakeId);
    request.addParameter("fid", String.valueOf(appMsgId));
    request.addParameter("appmsgid", String.valueOf(appMsgId));
    return toJsonObject(execute(request)).optInt("ret", -1) == 0;
  }

  /**
   * 增加单图文信息
   */
  public boolean addImageText(String title, String author, int fileId, String digest,
      String content, String source) throws MpException {
    return editImageText(null, title, author, fileId, digest, content, source);
  }

  /**
   * 编辑图文信息
   */
  public boolean editImageText(Integer appMsgId, String title, String author, int fileId,
      String digest, String content, String source) throws MpException {
    checkToken();
    String url = "https://mp.weixin.qq.com/cgi-bin/operate_appmsg";
    PostMethod request = new PostMethod(url);
    // request.setRequestHeader("Referer", "https://mp.weixin.qq.com/cgi-bin/operate_appmsg");
    request.addParameter("token", token);
    request.addParameter("lang", "zh_CN");
    request.addParameter("t", "ajax-response");
    request.addParameter("error", "false");
    request.addParameter("ajax", "1");
    request.addParameter("sub", "create");
    request.addParameter("count", "1");
    request.addParameter("AppMsgId", appMsgId == null ? "" : String.valueOf(appMsgId));
    request.addParameter("title0", title);
    request.addParameter("author0", author == null ? "" : author);
    request.addParameter("fileid0", String.valueOf(fileId));// 大图片建议尺寸：720像素 * 400像素封面
    request.addParameter("author0", author == null ? "" : author);
    request.addParameter("digest0", digest == null ? "" : digest);
    request.addParameter("content0", content);
    request.addParameter("sourceurl0", source == null ? "" : source);
    return toJsonObject(execute(request)).optInt("ret", -1) == 0;
  }

  /**
   * 删除图文
   */
  public boolean deleteImageText(int appMsgId) throws MpException {
    String url = "https://mp.weixin.qq.com/cgi-bin/operate_appmsg";
    PostMethod request = new PostMethod(url);
    request.addParameter("token", token);
    // request.addParameter("lang", "zh_CN");
    request.addParameter("t", "ajax-response");
    request.addParameter("ajax", "1");
    request.addParameter("sub", "del");
    request.addParameter("AppMsgId", String.valueOf(appMsgId));
    return toJsonObject(execute(request)).optInt("ret", -1) == 0;
  }

  /**
   * 上传图片
   */
  private int uploadImage(String type, byte[] image) throws MpException {
    checkToken();
    String url = "https://mp.weixin.qq.com/cgi-bin/uploadmaterial";
    url = url + "?token=" + token;
    url = url + "&lang=zh_CN";
    url = url + "&t=iframe-uploadfile";
    url = url + "&cgi=uploadmaterial";
    url = url + "&type=0";
    url = url + "&formId=";// "file_from_" + System.currentTimeMillis()
    PostMethod request = new PostMethod(url);
    request.addRequestHeader("Referer", "https://mp.weixin.qq.com/cgi-bin/indexpage");
    try {
      Part[] parts = new Part[] {new ByteArrayPart(image, "uploadfile", type)};
      MultipartRequestEntity entry = new MultipartRequestEntity(parts, request.getParams());
      request.setRequestEntity(entry);
      String html = execute(request, false);
      Matcher matcher = Pattern.compile("'(\\d+)'").matcher(html);
      if (html.contains("上传成功") && matcher.find()) {
        return Integer.parseInt(matcher.group(1));
      }
    } catch (IOException e) {
      throw new MpException(e);
    }

    throw new MpException("上传失败");
  }

  /**
   * 删除素材文件
   */
  private boolean deleteFile(int fileid) throws MpException {
    String url = "https://mp.weixin.qq.com/cgi-bin/modifyfile";
    PostMethod request = new PostMethod(url);
    request.addParameter("token", token);
    request.addParameter("lang", "zh_CN");
    request.addParameter("t", "ajax-response");
    request.addParameter("ajax", "1");
    request.addParameter("oper", "del");
    request.addParameter("fileid", String.valueOf(fileid));
    return toJsonObject(execute(request)).optInt("ret", -1) == 0;
  }

  private String execute(HttpMethod request) throws MpException {
    return execute(request, true);
  }

  private String execute(HttpMethod request, boolean form) throws MpException {
    request.addRequestHeader("Pragma", "no-cache");
    request.addRequestHeader("User-Agent",
        "Mozilla/5.0 (Macintosh; Intel Mac OS X 10.8; rv:23.0) Gecko/20100101 Firefox/23.0");
    if (form && request instanceof PostMethod) {
      request.addRequestHeader("Content-Type", "application/x-www-form-urlencoded; charset=UTF-8");
    }
    if (request.getRequestHeader("Referer") == null) {
      request.addRequestHeader("Referer", "https://mp.weixin.qq.com/");
    }

    try {
      int status = httpClient.executeMethod(request);
      if (status != HttpStatus.SC_OK) {
        throw new MpException(MpException.DEFAULT_CODE, String.valueOf(status));
      }
      return request.getResponseBodyAsString();
    } catch (HttpException e) {
      throw new MpException(e);
    } catch (IOException e) {
      throw new MpException(e);
    }
  }

  private JSONObject toJsonObject(String json) throws MpException {
    try {
      return new JSONObject(json);
    } catch (JSONException e) {
      throw new MpException(e);
    }
  }

}
