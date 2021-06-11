package top.ccxxh.live.recording;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.util.IOUtils;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import top.ccxh.httpclient.common.HttpResult;
import top.ccxh.httpclient.service.HttpClientService;
import top.ccxxh.live.po.RoomInfo;
import top.ccxxh.live.service.LiveService;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.text.SimpleDateFormat;
import java.util.Date;

/**
 * @author qing
 */
public class FlvRecording extends AbsFlvRecording {
    private final static Logger log = LoggerFactory.getLogger(FlvRecording.class);
    private final static SimpleDateFormat DATA_FORMAT = new SimpleDateFormat("yyyy-MM-dd HH-mm-ss");
    private final static SimpleDateFormat DATA_FORMAT_2 = new SimpleDateFormat("HH-mm-ss");
    private final static String SUFFIX = ".flv";
    private final byte[] buff = new byte[1024 * 4];
    private final static int MONITOR_TIME = 1000 * 10;

    public FlvRecording(Integer roomId, LiveService liveService, HttpClientService httpClientService, long maxSize) {
        super(roomId, maxSize);
        this.liveService = liveService;
        this.httpClientService = httpClientService;
    }


    /**
     * 处理live的服务
     */
    private final LiveService liveService;
    private final HttpClientService httpClientService;


    @Override
    public void recording() {
        final Date monitorStartTime = new Date();
        RoomInfo roomInfo = liveService.getRoomInfo(getRoomId());
        log.info(JSON.toJSONString(roomInfo));
        for (; !liveService.getLiveStatus(getRoomId()); ) {
            try {
                Thread.sleep(MONITOR_TIME);
                log.info("{}:未开播-{}", roomInfo.getuName(),DATA_FORMAT.format(monitorStartTime) );
            } catch (InterruptedException e) {}
        }
        boolean flag = false;
        addFileIndex();
        String file = roomInfo.getuName()+DATA_FORMAT.format(new Date()) + "の%s" + "[" + getFileIndex() + "]" + SUFFIX;
        String tempPath = file + ".temp";
        setNowPath(tempPath);
        String livePayUrl = liveService.getLivePayUrl(getRoomId());
        log.info("start:{}", livePayUrl);
        BufferedInputStream liveIn = null;
        try (
                HttpResult httpResult = httpClientService.get(livePayUrl);
                CloseableHttpResponse response = httpResult.getResponse();
                BufferedOutputStream fileOut = new BufferedOutputStream(new FileOutputStream(tempPath))
        ) {
            liveIn = new BufferedInputStream(response.getEntity().getContent());
            int len = -1;
            resetNow();
            while ((len = liveIn.read(buff)) != -1) {
                fileOut.write(buff, 0, len);
                if (addNow(len)) {
                    flag = true;
                    break;
                }
            }
        } catch (Exception e) {
            log.error(e.getMessage(), e);
        }
        IOUtils.close(liveIn);
        final File tempFile = new File(tempPath);
        if (tempFile.length() <= 0) {
            tempFile.delete();
        } else {
            String path = String.format(file, DATA_FORMAT_2.format(new Date()));
            tempFile.renameTo(new File(path));
            log.info("{}:over", path);
            addPathList(path);
        }
        if (!flag) {
            resetFileIndex();
            log.info("{}:等待重新开播", roomInfo.getuName());
        }
        recording();
    }
}
