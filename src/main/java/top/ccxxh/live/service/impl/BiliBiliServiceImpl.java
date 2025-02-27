package top.ccxxh.live.service.impl;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import top.ccxh.httpclient.common.HttpResult;
import top.ccxh.httpclient.service.HttpClientService;
import top.ccxxh.live.agent.CreatePool;
import top.ccxxh.live.constants.LiveSourceEnum;
import top.ccxxh.live.po.M3u8;
import top.ccxxh.live.po.RoomInfo;
import top.ccxxh.live.recording.AbsRecording;
import top.ccxxh.live.recording.FlvRecording;
import top.ccxxh.live.service.LiveService;

import java.util.HashMap;
import java.util.Map;

/**
 * @author qing
 */
@Service()
public class BiliBiliServiceImpl implements LiveService, CreatePool {
    private final static String ROOM_INFO_INIT_URL = "https://api.live.bilibili.com/room/v1/Room/room_init?id=%s";
    private final static String OLD_PAY_URL = "https://api.live.bilibili.com/room/v1/Room/playUrl?cid=%s&quality=3&platform=web";
    private final static String PAY_URL = "https://api.live.bilibili.com/xlive/web-room/v2/index/getRoomPlayInfo?room_id=%s&protocol=0,1&format=0,1,2&codec=0,1&qn=150&platform=web&ptype=8";
    private final static String ROOM_INFO_URL = "https://api.live.bilibili.com/xlive/web-room/v1/index/getInfoByRoom?room_id=%s";
    private final static String KEY_DATA = "data";
    private final static String KEY_LIVE_STATUS = "live_status";
    private final static String KEY_D_URL = "durl";
    private final static String KEY_URL = "url";
    private final static int LIVE = 1;
    private final static Logger log = LoggerFactory.getLogger(BiliBiliServiceImpl.class);
    @SuppressWarnings({"SpringJavaInjectionPointsAutowiringInspection", "SpringJavaAutowiredFieldsWarningInspection"})
    @Autowired
    private HttpClientService httpClientService;

    @Override
    public RoomInfo getRoomInfo(Integer id) {
        HttpResult httpResult = httpClientService.get(String.format(ROOM_INFO_URL, id));
        return bRoomInfo2RoomInfo(httpResult);
    }

    private RoomInfo bRoomInfo2RoomInfo(HttpResult httpResult) {
        JSONObject json = getJson(httpResult);
        RoomInfo result = new RoomInfo();
        JSONObject roomInfo = json.getJSONObject("room_info");
        JSONObject anchorInfo = json.getJSONObject("anchor_info").getJSONObject("base_info");
        result.setRoomTitle(roomInfo.getString("title"));
        result.setRoomId(roomInfo.getInteger("room_id"));
        result.setuId(roomInfo.getInteger("uid"));
        result.setSource(getSource());
        result.setuName(anchorInfo.getString("uname"));
        return result;
    }

    @Override
    public Boolean getLiveStatus(Integer id) {
        HttpResult httpResult = httpClientService.get(String.format(ROOM_INFO_INIT_URL, id));
        JSONObject result = getJson(httpResult);
        if (result == null) {
            //保障上面接口被拦截时能正常获取直播间状态后续应该加入代理
            httpResult = httpClientService.get(String.format(ROOM_INFO_URL, id));
            result = getJson(httpResult).getJSONObject("room_info");
        }
        return result.getIntValue(KEY_LIVE_STATUS) == LIVE;
    }

    @Override
    public String getM3u8Ulr(Integer id) {
        JSONArray stream = getStream(id);
        String payUrl = getPayUrl(stream.getJSONObject(1));
        HttpResult httpResult = httpClientService.get(payUrl);
        String entityStr = httpResult.getEntityStr();
        M3u8 m3u8 =M3u8.parse(payUrl, entityStr);
        return m3u8.getPayList().get(0);
    }

    @Override
    public String getFlvUrl(Integer id) {
        HttpResult httpResult = httpClientService.get(String.format(OLD_PAY_URL, id));
        JSONObject result = getJson(httpResult);
        JSONArray jsonArray = result.getJSONArray(KEY_D_URL);
        return jsonArray.getJSONObject(0).getString(KEY_URL);
    }

    @Override
    public LiveSourceEnum getSource() {
        return LiveSourceEnum.BILI_BILI;
    }

    @Override
    public Class<? extends AbsRecording> getRecording() {
        return FlvRecording.class;
    }

    @Override
    public long getSplitSize() {
        return (long) ((1000L * 1000L) * 400D);
    }

    private JSONObject getJson(HttpResult httpResult) {
        String entityStr = httpResult.getEntityStr();
        JSONObject result = JSON.parseObject(entityStr);

        return result != null ? result.getJSONObject(KEY_DATA) : null;
    }

    private String getNewFlvUrl(Integer id) {
        final JSONArray stream = getStream(id);
        return getPayUrl(stream.getJSONObject(0));

    }

    private JSONArray getStream(Integer id) {
        Map<String, String> header = new HashMap<>();
        header.put("Host", "api.live.bilibili.com");
        HttpResult httpResult = httpClientService.get(String.format(PAY_URL, id), null, header);
        final JSONObject data = getJson(httpResult);
        return data.getJSONObject("playurl_info").getJSONObject("playurl").getJSONArray("stream");
    }

    private String getPayUrl(JSONObject item) {

        final JSONObject format = item.getJSONArray("format").getJSONObject(0);
        final JSONObject codec = format.getJSONArray("codec").getJSONObject(0);
        final String baseUrl = codec.getString("base_url");
        final JSONObject urlInfo = codec.getJSONArray("url_info").getJSONObject(0);
        return urlInfo.getString("host") + baseUrl + urlInfo.getString("extra");
    }

    @Override
    public String getCheckUrl() {
        return "https://api.live.bilibili.com/room/v1/Room/room_init?id=22528847";
    }
}
