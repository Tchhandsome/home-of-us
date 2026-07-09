package com.homeofus.plant.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.stereotype.Component;

/**
 * 启动时修复历史养护提醒脏数据。
 *
 * @author tanchaohong
 */
@Component
public class PlantCareRepairRunner implements ApplicationRunner {

    private static final Logger log = LoggerFactory.getLogger(PlantCareRepairRunner.class);

    private final PlantService plantService;

    public PlantCareRepairRunner(PlantService plantService) {
        this.plantService = plantService;
    }

    @Override
    public void run(ApplicationArguments args) {
        int repairedCount = plantService.repairHistoricalCareSchedules();
        if (repairedCount > 0) {
            log.info("历史养护提醒修复完成，修正 {} 条记录", repairedCount);
        }
    }
}
