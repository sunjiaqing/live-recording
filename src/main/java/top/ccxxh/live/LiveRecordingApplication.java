package top.ccxxh.live;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;
import top.ccxxh.live.agent.AgentManager;
import top.ccxxh.live.agent.spider.Ip66Spider;
import top.ccxxh.live.config.LiveConfig;
import top.ccxxh.live.service.LiveService;

/**
 * @author qing
 */
@SpringBootApplication
@EnableScheduling
public class LiveRecordingApplication implements CommandLineRunner {


    @Autowired
    @Qualifier("biliBiliServiceImpl")
    private LiveService biliBiliService;
    @Autowired
    @Qualifier("misServiceImpl")
    private LiveService misServiceImpl;
    @Autowired
    private Ip66Spider ip66Spider;

    @Autowired
    private LiveConfig liveConfig;

    @Autowired
    private LiveContent liveContent;

    @Autowired
    private AgentManager agentManager;

    public static void main(String[] args) {
        SpringApplication.run(LiveRecordingApplication.class, args);
    }


    @Override
    public void run(String... args) throws Exception {
        agentManager.testStart();

        //liveContent.liveRecording(liveConfig.getBili(), biliBiliService);
        ///liveContent.liveRecording(liveConfig.getMis(), misServiceImpl);

    }


}
