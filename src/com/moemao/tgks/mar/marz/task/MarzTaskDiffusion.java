package com.moemao.tgks.mar.marz.task;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import net.sf.json.JSONArray;
import net.sf.json.JSONObject;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.beans.BeansException;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;

import com.moemao.tgks.common.core.spring.ContextUtil;
import com.moemao.tgks.common.tool.CommonConstant;
import com.moemao.tgks.common.tool.CommonUtil;
import com.moemao.tgks.mar.execute.MarzRequest;
import com.moemao.tgks.mar.krsmacard.entity.KrsmaCardEvt;
import com.moemao.tgks.mar.krsmacard.service.KrsmaCardService;
import com.moemao.tgks.mar.marz.entity.BossEvt;
import com.moemao.tgks.mar.marz.entity.CardEvt;
import com.moemao.tgks.mar.marz.entity.CardTagEvt;
import com.moemao.tgks.mar.marz.entity.DeckEvt;
import com.moemao.tgks.mar.marz.entity.ItemEvt;
import com.moemao.tgks.mar.marz.entity.MissionEvt;
import com.moemao.tgks.mar.marz.entity.PresentEvt;
import com.moemao.tgks.mar.marz.thread.MarzThreadPoolDiffusion;
import com.moemao.tgks.mar.marz.tool.MarzConstant;
import com.moemao.tgks.mar.marz.tool.MarzDataPool;
import com.moemao.tgks.mar.marz.tool.MarzUtil;
import com.moemao.tgks.mar.marzaccount.entity.MarzAccountEvt;
import com.moemao.tgks.mar.marzaccount.service.MarzAccountService;
import com.moemao.tgks.mar.marzitem.entity.MarzItemEvt;
import com.moemao.tgks.mar.marzitem.entity.MarzItemReq;
import com.moemao.tgks.mar.marzitem.service.MarzItemService;
import com.moemao.tgks.mar.marzlog.service.MarzLogService;
import com.moemao.tgks.mar.marzmap.entity.MarzMapEvt;
import com.moemao.tgks.mar.marzsetting.entity.MarzSettingEvt;
import com.moemao.tgks.mar.marzsetting.entity.MarzSettingReq;
import com.moemao.tgks.mar.marzsetting.service.MarzSettingService;
import com.moemao.tgks.mar.tool.MarConstant;
import com.moemao.tgks.moecode.tool.Util;

public class MarzTaskDiffusion implements Runnable, ApplicationContextAware
{
    public boolean running = true;
    
    private static Log logger = LogFactory.getLog(MarzTaskDiffusion.class);
    
    private MarzRequest request = MarzRequest.getInstance();
    
    private MarzAccountService marzAccountService;
    
    private MarzLogService marzLogService;
    
    private MarzSettingService marzSettingService;
    
    private MarzSettingEvt marzSettingEvt;
    
    private MarzItemService marzItemService;
    
    private KrsmaCardService marKrsmaCardService;
    
    private MarzAccountEvt account;
    
    private Map<String, JSONObject> map;
    
    private String sid;
    
    private int resultCode = MarzConstant.SUCCESS;
    
    private static int SLEEPTIME = 2 * 60 * 1000;
    
    /**
     * 账户的四职业第一卡组
     */
    private Map<String, DeckEvt> deckMap;
    
    private Map<String, String> pvpEndMap;
    
    private boolean itemUseFlag = false;
    
    private String itemUseId = "";
    
    /**
     * PVP打完时的标志
     */
    private boolean pvpChallengeOverFlag = false;
    
    private String arthurType = "3";
    
    /**
     * 登录失败次数
     */
    private int loginFailedTimes = 0;
    
    public MarzTaskDiffusion(MarzAccountEvt marzAccountEvt)
    {
        // 初始化一些参数
        account = marzAccountEvt;
        marzAccountService = (MarzAccountService) ContextUtil.getBean("mar_marzAccountService");
        marzLogService = (MarzLogService) ContextUtil.getBean("mar_marzLogService");
        marzSettingService = (MarzSettingService) ContextUtil.getBean("mar_marzSettingService");
        marzItemService = (MarzItemService) ContextUtil.getBean("mar_marzItemService");
        marKrsmaCardService = (KrsmaCardService) ContextUtil.getBean("mar_krsmaCardService");
        
        // PVP结束参数
        pvpEndMap = new HashMap<String, String>();
        pvpEndMap.put("1", MarConstant.PVPEND_1);
        pvpEndMap.put("2", MarConstant.PVPEND_2);
        pvpEndMap.put("3", MarConstant.PVPEND_3);
        pvpEndMap.put("4", MarConstant.PVPEND_4);
        
        // 默认线程调用的执行方法
        System.out.println("执行任务开始 ID：" + account.getTgksId());
    }

    @Override
    public void run()
    {
        Thread.currentThread().setName(MarConstant.MODULE_TAG + account.getTgksId());
        
        // 尽量保证流程上的简洁 run流程只负责调用以及返回失败时的处理 并不做各个条件判断的限制
        try
        {
            while (running)
            {
                
                // 为了防止出现点击上线按钮立即生成线程却跳出的情况 线程需要先暂停1秒
                Thread.sleep(1000);
                
                account = this.marzAccountService.queryMarzAccountById(account.getId());
                if (MarzConstant.MARZ_ACCOUNT_STATUS_0.equals(account.getStatus()))
                {
                    break;
                }
                
                // 每次循环检查点卡是否到期
                if (new Date().after(account.getEndTime()))
                {
                    this.marzLogService.marzLog(account, MarzConstant.MARZ_LOG_TYPE_0, account.getTgksId() + "已经到期，退出挂机...");
                    account.setStatus(MarzConstant.MARZ_ACCOUNT_STATUS_0);
                    this.marzAccountService.updateMarzAccount(account);
                    break;
                }
                
                // 更新设置
                this.initSetting();
                
                
                // 1、账号登陆
                if (CommonUtil.isEmpty(account.getSessionId()))
                {
                    resultCode = this.login();
                    
                    if (MarzConstant.RES_CODE_SUCCESS_0 != resultCode)
                    {
                        if (this.loginFailedTimes > 3)
                        {
                            this.marzLogService.marzLog(account, MarzConstant.MARZ_LOG_TYPE_0, "登录失败已达最大次数，退出挂机...");
                            this.offLine();
                            break;
                        }
                        
                        this.loginFailedTimes++;
                        
                        if (MarzConstant.RES_CODE_ERROR_M5 == resultCode)
                        {
                            this.marzLogService.marzLog(account, MarzConstant.MARZ_LOG_TYPE_0, "系统维护中，账号自动下线，请留意游戏公告等开服后手动上线...");
                            this.offLine();
                            break;
                        }
                        else if (MarzConstant.RES_CODE_ERROR_M7 == resultCode)
                        {
                            this.marzLogService.marzLog(account, MarzConstant.MARZ_LOG_TYPE_0, "账号被重新引继过，原存档文件失效，无法继续挂机需要重新绑定账号...");
                            this.offLine();
                            break;
                        }
                        else if (MarzConstant.RES_CODE_ERROR_M8 == resultCode)
                        {
                            this.marzLogService.marzLog(account, MarzConstant.MARZ_LOG_TYPE_0, "游戏客户端已更新，服务器需要同步更新，请关注公告...");
                            this.offLine();
                            break;
                        }
                        
                        this.sleep(SLEEPTIME);
                        continue;
                    }
                }
                else
                {
                    // 当前的逻辑 每次扫描出的任务如果包含sid 则跳过登录
                    // 在用户操作上下线的时候 将account的sid一同清空即可
                    // 如果sid已经失效 会在下面的homeShow中重新登录
                    sid = account.getSessionId();
                }
                
                if (Thread.currentThread().getName().contains(MarzConstant.OVER))
                {
                    break;
                }
                
                // 2、更新当前账号基础数据
                resultCode = this.baseInfo();
                
                if (MarzConstant.SUCCESS > resultCode)
                {
                    System.out.println("发生了错误！当前resultCode：" + resultCode);
                    
                    resultCode = this.login();
                    
                    if (MarzConstant.RES_CODE_SUCCESS_0 != resultCode)
                    {
                        if (this.loginFailedTimes > 3)
                        {
                            this.marzLogService.marzLog(account, MarzConstant.MARZ_LOG_TYPE_0, "登录失败已达最大次数，退出挂机...");
                            this.offLine();
                            break;
                        }
                        
                        this.loginFailedTimes++;
                        
                        if (MarzConstant.RES_CODE_ERROR_M5 == resultCode)
                        {
                            this.marzLogService.marzLog(account, MarzConstant.MARZ_LOG_TYPE_0, "系统维护中，账号自动下线，请留意游戏公告等开服后手动上线...");
                            this.offLine();
                            break;
                        }
                        else if (MarzConstant.RES_CODE_ERROR_M7 == resultCode)
                        {
                            this.marzLogService.marzLog(account, MarzConstant.MARZ_LOG_TYPE_0, "账号被重新引继过，原存档文件失效，无法继续挂机需要重新绑定账号...");
                            this.offLine();
                            break;
                        }
                        else if (MarzConstant.RES_CODE_ERROR_M8 == resultCode)
                        {
                            this.marzLogService.marzLog(account, MarzConstant.MARZ_LOG_TYPE_0, "游戏客户端已更新，服务器需要同步更新，请关注公告...");
                            this.offLine();
                            break;
                        }
                        
                        this.sleep(SLEEPTIME);
                        continue;
                    }
                    
                    resultCode = this.baseInfo();
                    
                    if (MarzConstant.SUCCESS > resultCode)
                    {
                        account.setSessionId("");
                        account.setRemark("");
                        this.marzAccountService.updateMarzAccount(account);
                        
                        this.sleep(SLEEPTIME);
                        continue;
                    }
                }
                
                if (Thread.currentThread().getName().contains(MarzConstant.OVER))
                {
                    break;
                }
                
                // 2-1、道具信息（嗑药买药也在这里面执行）
                resultCode = this.item();
                
                if (Thread.currentThread().getName().contains(MarzConstant.OVER))
                {
                    break;
                }
                
                // 3、探索
                resultCode = this.explore();
                
                if (Thread.currentThread().getName().contains(MarzConstant.OVER))
                {
                    break;
                }
                
                // 4、卡片处理
                resultCode = this.card();
                
                if (Thread.currentThread().getName().contains(MarzConstant.OVER))
                {
                    break;
                }
                
                // 5、打副本
                resultCode = this.battle();
                
                if (Thread.currentThread().getName().contains(MarzConstant.OVER))
                {
                    break;
                }
                
                // 6、PVP
                resultCode = this.pvp();
                
                // 最后要保存一下sessionId
                account.setSessionId(sid);
                
                // 保存之前都要读取数据库中最新的数据  防止脏数据发生
                this.saveAccount();
                
                this.marzLogService.marzLog(account, MarzConstant.MARZ_LOG_TYPE_0, account.getTgksId() + "本轮执行完毕，等待下一次执行...");
                
                if (Thread.currentThread().getName().contains(MarzConstant.OVER))
                {
                    break;
                }
                
                System.out.println(Thread.currentThread().getName() + "本次任务执行完成 进入等待时间 [" + new SimpleDateFormat("yyyy-MM-dd HH:mm:ss").format(new Date()) + "]");
                
                SLEEPTIME = ((2 * 60) + (MarzThreadPoolDiffusion.getInstance().getMarzThreadNum() * 6)) * 1000;
                
                // 等待时间间隔
                this.sleep(SLEEPTIME);
            }
        }
        catch (Exception e)
        {
            System.out.println(MarConstant.MODULE_TAG + account.getTgksId() + "任务发生异常，即将退出...");
        }
        
        this.offLine();
        
        this.marzLogService.marzLog(account, MarzConstant.MARZ_LOG_TYPE_0, account.getTgksId() + "已经下线...");
        System.out.println(MarConstant.MODULE_TAG + account.getTgksId() + "线程已关闭...");
        Thread.currentThread().setName(MarConstant.MODULE_TAG + MarzConstant.OVER);
    }
    
    private void offLine()
    {
        // 初始化状态
        account.setStatus(MarzConstant.MARZ_ACCOUNT_STATUS_0);
        account.setAp(0);
        account.setApMax(0);
        account.setBp(0);
        account.setBpMax(0);
        //account.setCardNum(0);
        //account.setCardMax(0);
        account.setCoin(0);
        account.setFp(0);
        account.setGold(0);
        account.setSessionId("");
        account.setIpAddress("");
        account.setRemark("");
        this.marzAccountService.updateMarzAccount(account);
    }
    
    private void initSetting()
    {
        String tgksId = account.getTgksId();
        marzSettingEvt = new MarzSettingEvt();
        marzSettingEvt.setTgksId(tgksId);
        MarzSettingReq marzSettingReq = new MarzSettingReq();
        marzSettingReq.setTgksId(tgksId);
        
        List<MarzSettingEvt> marzSettinglist = this.marzSettingService.queryMarzSetting(marzSettingReq);
        
        if (!CommonUtil.isEmpty(marzSettinglist))
        {
            for (MarzSettingEvt setting : marzSettinglist)
            {
                if (MarzConstant.VALIDATE_SETTING_EXPLORE == Integer.parseInt(setting.getName()))
                {
                    marzSettingEvt.setExplore(setting.getValue());
                }
                else if (MarzConstant.VALIDATE_SETTING_CARDSELL == Integer.parseInt(setting.getName()))
                {
                    marzSettingEvt.setCardSell(setting.getValue());
                }
                else if (MarzConstant.VALIDATE_SETTING_CARDSELL_COMMON == Integer.parseInt(setting.getName()))
                {
                    marzSettingEvt.setCardSellCommon(setting.getValue());
                }
                else if (MarzConstant.VALIDATE_SETTING_CHIARIFUSION == Integer.parseInt(setting.getName()))
                {
                    marzSettingEvt.setCardFusion(setting.getValue());
                }
                else if (MarzConstant.VALIDATE_SETTING_BATTLE == Integer.parseInt(setting.getName()))
                {
                    marzSettingEvt.setBattle(setting.getValue());
                }
                else if (MarzConstant.VALIDATE_SETTING_BATTLE_NOWASTE == Integer.parseInt(setting.getName()))
                {
                    marzSettingEvt.setBattleNowaste(setting.getValue());
                }
                else if (MarzConstant.VALIDATE_SETTING_BATTLE_NOWASTE_BOSSID == Integer.parseInt(setting.getName()))
                {
                    marzSettingEvt.setBattleNowasteBossId(setting.getValue());
                }
                else if (MarzConstant.VALIDATE_SETTING_BATTLE_GET_STONE == Integer.parseInt(setting.getName()))
                {
                    marzSettingEvt.setBattleGetStone(setting.getValue());
                }
                else if (MarzConstant.VALIDATE_SETTING_PVP == Integer.parseInt(setting.getName()))
                {
                    marzSettingEvt.setPvp(setting.getValue());
                }
                else if (MarzConstant.VALIDATE_SETTING_FAMEFUSION == Integer.parseInt(setting.getName()))
                {
                    marzSettingEvt.setFameFusion(setting.getValue());
                }
                else if (MarzConstant.VALIDATE_SETTING_COINGACHA == Integer.parseInt(setting.getName()))
                {
                    marzSettingEvt.setCoinGacha(setting.getValue());
                }
                else if (MarzConstant.VALIDATE_SETTING_COINGACHA_GACHAID == Integer.parseInt(setting.getName()))
                {
                    marzSettingEvt.setCoinGachaGachaId(setting.getValue());
                }
                else if (MarzConstant.VALIDATE_SETTING_AUTOUSEBPPOTION == Integer.parseInt(setting.getName()))
                {
                    marzSettingEvt.setAutoUseBPPotion(setting.getValue());
                }
                else if (MarzConstant.VALIDATE_SETTING_AUTOUSEBPPOTION_BPLIMIT == Integer.parseInt(setting.getName()))
                {
                    marzSettingEvt.setAutoUseBPPotionBPLimit(setting.getValue());
                }
                else if (MarzConstant.VALIDATE_SETTING_AUTOUSEBPPOTION_ITEMID == Integer.parseInt(setting.getName()))
                {
                    marzSettingEvt.setAutoUseBPPotionItemId(setting.getValue());
                }
                else if (MarzConstant.VALIDATE_SETTING_AUTOBUYBPPOTION == Integer.parseInt(setting.getName()))
                {
                    marzSettingEvt.setAutoBuyBPPotion(setting.getValue());
                }
                else if (MarzConstant.VALIDATE_SETTING_CARDSELL_EVO == Integer.parseInt(setting.getName()))
                {
                    marzSettingEvt.setCardSellEvo(setting.getValue());
                }
                else if (MarzConstant.VALIDATE_SETTING_CARDSELL_EVONUM == Integer.parseInt(setting.getName()))
                {
                    marzSettingEvt.setCardSellEvoNum(setting.getValue());
                }
                else if (MarzConstant.VALIDATE_SETTING_BATTLE_LEVEL_UP == Integer.parseInt(setting.getName()))
                {
                    marzSettingEvt.setBattleLevelUp(setting.getValue());
                }
                else if (MarzConstant.VALIDATE_SETTING_AUTOPRESENTRECV == Integer.parseInt(setting.getName()))
                {
                    marzSettingEvt.setAutoPresentRecv(setting.getValue());
                }
            }
        }
    }
    
    /**
     * 
     * @Title: sleep
     * @Description: 循环等待
     * @param sleepTime
     * @return void 返回类型
     * @throws
     */
    private void sleep(int sleepTime)
    {
        try
        {
            Thread.sleep(sleepTime);
        }
        catch (Exception e)
        {
            
        }
    }
    
    /**
     * 
     * @Title: login
     * @Description: 账户登陆 login+connect
     * @return void 返回类型
     * @throws
     */
    private int login()
    {
        try
        {
            if (MarzConstant.MARZ_ACCOUNT_TYPE_0.equals(account.getType()))
            {
            	request.authCheckIOS(account.getUuid());
                map = request.loginIOS(account.getUuid(), account.getHashToken());
            }
            else if (MarzConstant.MARZ_ACCOUNT_TYPE_1.equals(account.getType()))
            {
            	request.authCheckAndroid(account.getUuid());
                map = request.loginAndroid(account.getUuid(), account.getHashToken());
            }
            else if (MarzConstant.MARZ_ACCOUNT_TYPE_2.equals(account.getType()))
            {
            	request.authCheckAndroid(account.getUuid());
                map = request.loginAndroidSE(account.getUuid(), account.getHashToken());
            }
            else
            {
            	request.authCheckIOS(account.getUuid());
                map = request.loginIOS(account.getUuid(), account.getHashToken());
            }
            
            resultCode = map.get(MarzConstant.JSON_TAG_RESCODE).getInt(MarzConstant.JSON_TAG_RESCODE);
            
            this.marzLogService.marzLog(account, MarzConstant.MARZ_LOG_TYPE_0, "账号登录" + MarzUtil.resultCodeStr(resultCode));
            
            sid = map.get(MarzConstant.JSON_TAG_SID).getString(MarzConstant.JSON_TAG_SID);
            
            map = request.connect(sid);
            
            sid = map.get(MarzConstant.JSON_TAG_SID).getString(MarzConstant.JSON_TAG_SID);
            
            account.setSessionId(sid);
        }
        catch (Exception e)
        {
            System.out.println("登陆失败！退出任务");
            return MarzConstant.FAILED;
        }
        
        return resultCode;
    }
    
    private int baseInfo()
    {
        try
        {
            map = request.homeShow(sid);
            
            resultCode = map.get(MarzConstant.JSON_TAG_RESCODE).getInt(MarzConstant.JSON_TAG_RESCODE);
            
            //this.marzLogService.marzLog(account, MarzConstant.MARZ_LOG_TYPE_0, "账号基本信息更新" + MarzUtil.resultCodeStr(resultCode));
            
            if (MarzConstant.RES_CODE_SUCCESS_0 == resultCode)
            {
                this.loginFailedTimes = 0;
                
                sid = map.get(MarzConstant.JSON_TAG_SID).getString(MarzConstant.JSON_TAG_SID);
                
                account.setSessionId(sid);
                
                JSONObject user = map.get(MarzConstant.JSON_TAG_HOMWSHOW).getJSONObject("user");

                this.jsonUser(account, user);
                
                this.saveAccount();
            }
        }
        catch (Exception e)
        {
            this.loginFailedTimes++;
            return MarzConstant.FAILED;
        }
        
        return resultCode;
    }
    
    private int item()
    {
        try
        {
            // 匹配物品信息
            MarzItemReq marzItemReq = new MarzItemReq();
            List<MarzItemEvt> marzItemList = this.marzItemService.queryMarzItem(marzItemReq);
            
            // add by ken 20150907 for presentRecv time limit
            if (validateSetting(MarzConstant.VALIDATE_SETTING_AUTOPRESENTRECV) && MarzUtil.inPvpResetTime())
            {
                // 先看看任务里面有没有可收取的
                map = request.missionShow(sid);
                resultCode = map.get(MarzConstant.JSON_TAG_RESCODE).getInt(MarzConstant.JSON_TAG_RESCODE);
                
                if (MarzConstant.RES_CODE_SUCCESS_0 == resultCode)
                {
                    sid = map.get(MarzConstant.JSON_TAG_SID).getString(MarzConstant.JSON_TAG_SID);
                    
                    // 处理报文整理为mission的List
                    JSONObject missionShow = map.get(MarzConstant.JSON_TAG_MISSIONSHOW);
                    JSONArray missions = missionShow.getJSONArray("missions");
                    List<MissionEvt> missionList = new ArrayList<MissionEvt>();
                    JSONObject missionJSON;
                    
                    for (int i = 0, size = missions.size(); i < size; i++)
                    {
                        missionJSON = JSONObject.fromObject(missions.get(i));
                        missionList.add(new MissionEvt(missionJSON));
                    }
                    
                    for (MissionEvt m : missionList)
                    {
                        // add by ken 20150907 for quest select job
                        if("2001001".equals(m.getMissionid()) && "0".equals(m.getState()))
                        {
                            this.arthurType = "1";
                        }
                        else if("2001002".equals(m.getMissionid()) && "0".equals(m.getState()))
                        {
                            this.arthurType = "2";
                        }
                        else if("2001003".equals(m.getMissionid()) && "0".equals(m.getState()))
                        {
                            this.arthurType = "3";
                        }
                        else if("2001004".equals(m.getMissionid()) && "0".equals(m.getState()))
                        {
                            this.arthurType = "4";
                        }
                        
                        // 这里过滤任务只做收取任务奖励使用
                        if ("1".equals(m.getState()))
                        {
                            map = this.request.missionReward(sid, m.getMissionid());
                            sid = map.get(MarzConstant.JSON_TAG_SID).getString(MarzConstant.JSON_TAG_SID);
                            this.marzLogService.marzLog(account, MarzConstant.MARZ_LOG_TYPE_0, "收取任务奖励：" + m.getTitle());
                        }
                    }
                }
                
                // 下面开始收礼物箱
                map = request.presentBoxShow(sid);
                
                resultCode = map.get(MarzConstant.JSON_TAG_RESCODE).getInt(MarzConstant.JSON_TAG_RESCODE);
                
                List<PresentEvt> presentList = new ArrayList<PresentEvt>();
                
                if (MarzConstant.RES_CODE_SUCCESS_0 == resultCode)
                {
                    sid = map.get(MarzConstant.JSON_TAG_SID).getString(MarzConstant.JSON_TAG_SID);
                    
                    JSONObject presentBoxShow = map.get(MarzConstant.JSON_TAG_PRESENTBOXSHOW);
                    JSONArray presents = presentBoxShow.getJSONArray("presents");
                    JSONObject presentJSON;
                    
                    for (int i = 0, size = presents.size(); i < size; i++)
                    {
                        presentJSON = JSONObject.fromObject(presents.get(i));
                        presentList.add(new PresentEvt(presentJSON));
                    }
                    
                    // 取出有时间限制的礼物
                    for (PresentEvt present : presentList)
                    {
                        // 仅对有时间限制的道具进行取出
                        if (present.getLimit_tm() > 0)
                        {
                            map = request.presentBoxRecv(sid, present.getPresentid());
                            
                            resultCode = map.get(MarzConstant.JSON_TAG_RESCODE).getInt(MarzConstant.JSON_TAG_RESCODE);
                            
                            if (MarzConstant.RES_CODE_SUCCESS_0 == resultCode)
                            {
                                sid = map.get(MarzConstant.JSON_TAG_SID).getString(MarzConstant.JSON_TAG_SID);
                                
                                this.marzLogService.marzLog(account, MarzConstant.MARZ_LOG_TYPE_3, "收取礼物成功！" + present.getComment());
                            }
                        }
                    }
                }
            }
            
            map = request.itemShow(sid);
            
            resultCode = map.get(MarzConstant.JSON_TAG_RESCODE).getInt(MarzConstant.JSON_TAG_RESCODE);
            
            List<ItemEvt> itemList = new ArrayList<ItemEvt>();
            
            if (MarzConstant.RES_CODE_SUCCESS_0 == resultCode)
            {
                sid = map.get(MarzConstant.JSON_TAG_SID).getString(MarzConstant.JSON_TAG_SID);
                
                JSONObject itemShow = map.get(MarzConstant.JSON_TAG_ITEMSHOW);
                JSONArray items = itemShow.getJSONArray("items");
                JSONObject itemJSON;
                
                for (int i = 0, size = items.size(); i < size; i++)
                {
                    itemJSON = JSONObject.fromObject(items.get(i));
                    itemList.add(new ItemEvt(itemJSON));
                }
                
                account.setItemList(itemList);
                
                // 记录道具信息
                account.setItemInfo(MarzUtil.getItemInfo(itemList, marzItemList));
                this.saveAccount();
                
                // 开始自动嗑药逻辑处理
                // 嗑药需要保证开关打开 BP小于阀值
                if (validateSetting(MarzConstant.VALIDATE_SETTING_AUTOUSEBPPOTION) && account.getBp() < Integer.parseInt(marzSettingEvt.getAutoUseBPPotionBPLimit()))
                {
                    // 查看当前药水数量
                    int bpNum = 0;
                    itemUseFlag = false;
                    for (ItemEvt item : itemList)
                    {
                        // update by ken 20150907 for change id to id1,id2
                        //if (item.getItemId().equals(marzSettingEvt.getAutoUseBPPotionItemId()))
                        if (marzSettingEvt.getAutoUseBPPotionItemId().contains(item.getItemId()))
                        {
                            bpNum = item.getNum();
                            
                            if (bpNum == 0)
                            {
                                continue;
                            }
                            
                            this.itemUseId = item.getItemId();
                            break;
                        }
                    }
                    
                    if (bpNum > 0)
                    {
                        this.itemUseFlag = true;
                    }
                    // 如果药水已经用完 看是否开启了自动买药
                    else if (0 == bpNum)
                    {
                        // 仅当开启了买药并且设定自动可ID为1000的大药时才会自动买药
                        if (validateSetting(MarzConstant.VALIDATE_SETTING_AUTOBUYBPPOTION)
                                && MarConstant.ITEM_ID_BP_RECOVER_FULL.equals(marzSettingEvt.getAutoUseBPPotionItemId()))
                        {
                            // 如果石头足够
                            if (account.getCoin() >= 5)
                            {
                                map = request.itemShopBuy(sid, marzSettingEvt.getAutoUseBPPotionItemId());
                                
                                resultCode = map.get(MarzConstant.JSON_TAG_RESCODE).getInt(MarzConstant.JSON_TAG_RESCODE);
                                
                                if (MarzConstant.RES_CODE_SUCCESS_0 == resultCode)
                                {
                                    this.marzLogService.marzLog(account, MarzConstant.MARZ_LOG_TYPE_3, "药水够买成功！");
                                    sid = map.get(MarzConstant.JSON_TAG_SID).getString(MarzConstant.JSON_TAG_SID);
                                    this.itemUseFlag = true;
                                }
                                else
                                {
                                    // 购买失败
                                    this.marzLogService.marzLog(account, MarzConstant.MARZ_LOG_TYPE_3, "药水够买失败！");
                                }
                            }
                            else
                            {
                                if (MarConstant.ITEM_ID_BP_RECOVER_FULL.equals(marzSettingEvt.getAutoUseBPPotionItemId()) || MarConstant.ITEM_ID_BP_RECOVER_HALF.equals(marzSettingEvt.getAutoUseBPPotionItemId()))
                                {
                                    this.marzLogService.marzLog(account, MarzConstant.MARZ_LOG_TYPE_3, "药水已经用完，且水晶不够买药！");
                                }
                            }
                        }
                        else
                        {
                            // 药水已经用完 并且没有开买药 记录日志直接跳出
                            if (MarConstant.ITEM_ID_BP_RECOVER_FULL.equals(marzSettingEvt.getAutoUseBPPotionItemId()) || MarConstant.ITEM_ID_BP_RECOVER_HALF.equals(marzSettingEvt.getAutoUseBPPotionItemId()))
                            {
                                this.marzLogService.marzLog(account, MarzConstant.MARZ_LOG_TYPE_3, "药水已经用完，且未开启自动买药！");
                            }
                        }
                    }
                    
                    // 喝药水转移到打副本那块逻辑去了
                }
            }
            
            // 下面开始自动抽硬币
            if (validateSetting(MarzConstant.VALIDATE_SETTING_COINGACHA) && !CommonUtil.isEmpty(account.getGachaHash()) && itemList.size() > 0
                    && account.getCardNum() < account.getCardMax())
            {
                // 抽奖名称=硬币ID=消耗数量=gachaId=payType
                String gachaInfo[] = marzSettingEvt.getCoinGachaGachaId().split(MarConstant.KRSMA_SPLIT);
                String gachaName = gachaInfo[0];
                String itemId = gachaInfo[1];
                int costNum = Integer.parseInt(gachaInfo[2]);
                String gachaId = gachaInfo[3];
                String payType = gachaInfo[4];
                
                ItemEvt coin = null;
                
                for (ItemEvt item : itemList)
                {
                    if (itemId.equals(item.getItemId()))
                    {
                        coin = item;
                        break;
                    }
                }
                
                // 如果没有硬币信息或者硬币数量不足 直接退出
                if (null != coin && coin.getNum() >= costNum)
                {
                    map = request.gachaPlay(sid, gachaId, payType, account.getGachaHash());
                    
                    resultCode = map.get(MarzConstant.JSON_TAG_RESCODE).getInt(MarzConstant.JSON_TAG_RESCODE);
                    
                    if (MarzConstant.RES_CODE_SUCCESS_0 == resultCode)
                    {
                        sid = map.get(MarzConstant.JSON_TAG_SID).getString(MarzConstant.JSON_TAG_SID);
                        
                        JSONObject gachaPlay = map.get(MarzConstant.JSON_TAG_GACHAPLAY);
                        JSONArray cards = gachaPlay.getJSONArray("cards");
                        JSONObject cardJSON;
                        
                        List<CardEvt> gachaCardList = new ArrayList<CardEvt>();
                        
                        for (int i = 0, size = cards.size(); i < size; i++)
                        {
                            cardJSON = JSONObject.fromObject(cards.get(i));
                            gachaCardList.add(new CardTagEvt(cardJSON));
                        }
                        
                        this.marzLogService.marzLog(account, MarzConstant.MARZ_LOG_TYPE_7, "自动抽卡 " + gachaName + " " + MarzUtil.getFaceImageUrlByList(gachaCardList));
                    }
                }
            }
        }
        catch (Exception e)
        {
            return MarzConstant.FAILED;
        }
        
        return resultCode;
    }
    
    private int explore()
    {
        // 在开头加入条件限制
        if (!validateSetting(MarzConstant.VALIDATE_SETTING_EXPLORE) || account.getAp() == 0 || (account.getBpMax() - account.getBp()) < 6)
        {
            return MarzConstant.SUCCESS;
        }
        
        try
        {
            // 接口设计的是可以根据职业来做 不过这个没啥意义 就写死了 | 纪念妹妹又活啦！
            request.exploreStart(sid, this.arthurType, "0");
            
            map = request.exploreEnd(sid);
            
            resultCode = map.get(MarzConstant.JSON_TAG_RESCODE).getInt(MarzConstant.JSON_TAG_RESCODE);
            
            this.marzLogService.marzLog(account, MarzConstant.MARZ_LOG_TYPE_2, "探索" + MarzUtil.resultCodeStr(resultCode));
            
            if (MarzConstant.RES_CODE_SUCCESS_0 == resultCode)
            {
                sid = map.get(MarzConstant.JSON_TAG_SID).getString(MarzConstant.JSON_TAG_SID);
                
                JSONObject user = map.get(MarzConstant.JSON_TAG_EXPLOREEND).getJSONObject("user");
                this.jsonUser(account, user);
                this.saveAccount();
            }
        }
        catch (Exception e)
        {
            return MarzConstant.FAILED;
        }
        
        return resultCode;
    }
    
    /**
     * 
     * @Title: card
     * @Description: 卡片处理 合成 出售等
     * @return
     * @return int 返回类型
     * @throws
     */
    private int card()
    {
        try
        {
            // 更新卡片信息
            map = request.cardShow2(sid);
            
            resultCode = map.get(MarzConstant.JSON_TAG_RESCODE).getInt(MarzConstant.JSON_TAG_RESCODE);
            
            //this.marzLogService.marzLog(account, MarzConstant.MARZ_LOG_TYPE_0, "卡片信息更新" + MarzUtil.resultCodeStr(resultCode));
            
            if (MarzConstant.RES_CODE_SUCCESS_0 == resultCode)
            {
                sid = map.get(MarzConstant.JSON_TAG_SID).getString(MarzConstant.JSON_TAG_SID);
                
                JSONObject cardShowJSON = map.get(MarzConstant.JSON_TAG_CARDSHOW);
                
                // cardShow2接口中 cards → 0
                JSONArray cardsJSON = cardShowJSON.getJSONArray("0");
                
                List<CardEvt> cardList = new ArrayList<CardEvt>();
                JSONObject cardJSON;
                
                for (int i = 0, size = cardsJSON.size(); i < size; i++)
                {
                    cardJSON = JSONObject.fromObject(cardsJSON.get(i));
                    cardList.add(new CardEvt(cardJSON));
                }
                
                // add by Ken 2015-7-9 进化材料单独变成数量统计
                // update by Ken 2015-11-06 狗粮被一起分到材料中变为数量统计
                // cardShow2接口中的1
                JSONArray chiarisJSON = cardShowJSON.getJSONArray("1");
                
                Map<String, Integer> chiariMap = new HashMap<String, Integer>();
                JSONObject chiariJSON;
                
                for (int i = 0, size = chiarisJSON.size(); i < size; i++)
                {
                    chiariJSON = JSONObject.fromObject(chiarisJSON.get(i));
                    chiariMap.put(chiariJSON.getString("0"), chiariJSON.getInt("1"));
                }
                
                // add by Ken 2015-6-3 for PVP & Solo
                // cardShow2接口中 decks → 2
                JSONArray decksJSON = cardShowJSON.getJSONArray("2");
                deckMap = new HashMap<String, DeckEvt>();
                JSONObject deckJSON;
                DeckEvt deckEvt;
                
                for (int i = 0, size = decksJSON.size(); i < size; i++)
                {
                    deckJSON = JSONObject.fromObject(decksJSON.get(i));
                    deckEvt = new DeckEvt(deckJSON);
                    if ("1".equals(deckEvt.getArthur_type()) && "0".equals(deckEvt.getDeck_idx()) && "1".equals(deckEvt.getJob_type()))
                    {
                        // 佣兵第一卡组
                        deckMap.put("1", deckEvt);
                    }
                    else if ("2".equals(deckEvt.getArthur_type()) && "0".equals(deckEvt.getDeck_idx()) && "2".equals(deckEvt.getJob_type()))
                    {
                        // 富豪第一卡组
                        deckMap.put("2", deckEvt);
                    }
                    else if ("3".equals(deckEvt.getArthur_type()) && "0".equals(deckEvt.getDeck_idx()) && "3".equals(deckEvt.getJob_type()))
                    {
                        // 盗贼第一卡组
                        deckMap.put("3", deckEvt);
                    }
                    else if ("4".equals(deckEvt.getArthur_type()) && "0".equals(deckEvt.getDeck_idx()) && "4".equals(deckEvt.getJob_type()))
                    {
                        // 歌姬第一卡组
                        deckMap.put("4", deckEvt);
                    }
                }
                
                // 这三个用来调用接口
                List<String> cardSellIdList = new ArrayList<String>();
                List<String> fameFusionIdList = new ArrayList<String>();
                
                // 这三个用来记录日志以及从cardList中remove掉
                List<CardEvt> cardSellCardList = new ArrayList<CardEvt>();
                List<CardEvt> fameFusionCardList = new ArrayList<CardEvt>();
                
                // 自动卖卡
                if (validateSetting(MarzConstant.VALIDATE_SETTING_CARDSELL))
                {
                    // 查询用户设定的售卡列表
                    List<String> userSellList = MarzUtil.stringToList(account.getSellCardIds());
                    
                    if (validateSetting(MarzConstant.VALIDATE_SETTING_CARDSELL_COMMON))
                    {
                        // 金币卡
                        //userSellList.add("20000026");
                        //userSellList.add("20000027");
                        //userSellList.add("20000028");
                        //userSellList.add("20000029");
                        // 蓝狗粮
                        //userSellList.add("20000001");
                        // 2星进化素材
                        //userSellList.add("20000009");
                        //userSellList.add("20000008");
                        //userSellList.add("20000007");
                        //userSellList.add("20000006");
                        //userSellList.add("20000005");
                    }
                    
                    // update by ken 20150622 将卖卡逻辑改为单次循环卖光
                    do
                    {
                        // 先清空List保证数据准确性 如果List为空时会跳出
                        cardSellIdList.clear();
                        cardSellCardList.clear();
                        
                    	// 遍历所有卡片 把需要出售的卡片ID放入cardSellList
                        for (CardEvt card : cardList)
                        {
                            // 只能出售未锁定以及是1级的卡
                            if (0 == card.getIs_lock() && 1 == card.getLv())
                            {
                                // 先卖 出售列表中的卡
                                if (userSellList.contains(card.getCardid()))
                                {
                                    cardSellIdList.add(card.getUniqid());
                                    cardSellCardList.add(card);
                                }
                                // 然后卖一些基础的垃圾卡 10~30
                                else if (validateSetting(MarzConstant.VALIDATE_SETTING_CARDSELL_COMMON)
                                        && card.getLv_max() >= 10 && card.getLv_max() <= 30)
                                {
                                    cardSellIdList.add(card.getUniqid());
                                    cardSellCardList.add(card);
                                }
                            }
                            
                            // 当出售的卡片满10张时 跳出
                            if (cardSellIdList.size() == 10)
                            {
                                break;
                            }
                        }
                        
                        // 组装卡牌ID调用cardSell请求
                        if (cardSellIdList.size() > 0)
                        {
                            map = request.cardSell(sid, MarzUtil.listToString(cardSellIdList));
                            
                            resultCode = map.get(MarzConstant.JSON_TAG_RESCODE).getInt(MarzConstant.JSON_TAG_RESCODE);
                            
                            if (MarzConstant.RES_CODE_SUCCESS_0 == resultCode)
                            {
                                sid = map.get(MarzConstant.JSON_TAG_SID).getString(MarzConstant.JSON_TAG_SID);
                                
                                account.setCardNum(map.get(MarzConstant.JSON_TAG_CARDSELL).getInt("card_num"));
                                account.setGold(account.getGold() + map.get(MarzConstant.JSON_TAG_CARDSELL).getInt("get_gold"));
                                
                                this.marzLogService.marzLog(account, MarzConstant.MARZ_LOG_TYPE_5, "已卖出卡片 " + MarzUtil.getFaceImageUrlByList(cardSellCardList));
                                
                                // 卖出卡片之后从卡组列表中删除已经出售的
                                cardList.removeAll(cardSellCardList);
                            }
                        }
                    }
                    while (MarzConstant.RES_CODE_SUCCESS_0 == resultCode && cardSellIdList.size() > 0);
                }
                
                // 名声合成
                if (validateSetting(MarzConstant.VALIDATE_SETTING_FAMEFUSION) && !CommonUtil.isEmpty(account.getFameCardIds()))
                {
                    // 查出已经设定好的名声合成卡片ID的List，然后遍历整个卡组，把每种卡放入Map<String, List<CardEvt>>中，之后再处理每种卡片
                	Map<String, List<CardEvt>> fameFusionMap = new HashMap<String, List<CardEvt>>();
                	List<String> userFameList = MarzUtil.stringToList(account.getFameCardIds());
                	// 合成主体卡
                	CardEvt baseFameCard = null;
                	List<CardEvt> fameCardList;
                	
                	// 把所有需要名声合成的卡ID整理成map
                	for (String fameCardId : userFameList)
                	{
                		fameFusionMap.put(fameCardId, new ArrayList<CardEvt>());
                	}
                	
                	// 然后遍历整个卡组 将需要名声合成的卡片放入对应map里的List中
                	for (CardEvt card : cardList)
                	{
                		if (userFameList.contains(card.getCardid()))
                		{
                			fameFusionMap.get(card.getCardid()).add(card);
                		}
                	}
                	
                	// 循环遍历Map中每个List，每次遍历完一个List就合成一次
                	for (String key : fameFusionMap.keySet())
                	{
                	    baseFameCard = null;
                		fameFusionIdList.clear();
    					fameFusionCardList.clear();
                		
                		fameCardList = fameFusionMap.get(key);
                		
                		if (fameCardList.size() < 2)
                		{
                			// 名声合成至少2张以上
                			continue;
                		}
                		
                		// 查询该卡的进化信息
                		KrsmaCardEvt krsmaCard = this.marKrsmaCardService.queryKrsmaCardByCardId(fameCardList.get(0).getCardid());
                		if (null == krsmaCard)
                		{
                			continue;
                		}
                		
            			// 先查询是否存在进化过的base卡
            			for (CardEvt card : cardList)
            			{
            				if (krsmaCard.getEvoCardId().equals(card.getCardid()))
            				{
            				    if (60 == card.getLv_max())
            				    {
            				        if (card.getFame() < 100)
                                    {
                                        baseFameCard = card;
                                    }
            				    }
            				    else if (50 == card.getLv_max())
                                {
                                    if (card.getFame() < 90)
                                    {
                                        baseFameCard = card;
                                    }
                                }
            				    else
            				    {
            				        if (card.getFame() < 100)
                                    {
                                        baseFameCard = card;
                                    }
            				    }
            				}
            			}
                		
                		for (CardEvt card : fameCardList)
                		{
                			if (null == baseFameCard)
                			{
                				// 如果没有找到进化过的卡，则拿第一张作为基础卡，基础卡只需要校验声望是否已经满了
                			    if (50 == card.getLv_max())
                                {
                    			    if (card.getFame() < 90)
                    				{
                    					baseFameCard = card;
                    				}
                                }
                			    else if (40 == card.getLv_max())
                                {
                                    if (card.getFame() < 70)
                                    {
                                        baseFameCard = card;
                                    }
                                }
                                else
                                {
                                    if (card.getFame() < 100)
                                    {
                                        baseFameCard = card;
                                    }
                                }
                			}
                			else
                			{
                				// 用来喂的卡必须为未锁上
                				if (0 == card.getIs_lock() && fameFusionIdList.size() <= 8)
                				{
                					fameFusionIdList.add(card.getUniqid());
                					fameFusionCardList.add(card);
                				}
                				
                				if (null != baseFameCard && fameFusionIdList.size() == 8)
                				{
                					break;
                				}
                			}
                		}
                		
                		// 满足合成条件
                		// update by ken 20160307 for baseCardId会在合成列表中
                		if (null != baseFameCard && fameFusionIdList.size() > 0 && !fameFusionIdList.contains(baseFameCard.getUniqid()))
                		{
                			// 调用合成接口
                			map = request.cardFusion(sid, baseFameCard.getUniqid(), MarzUtil.listToString(fameFusionIdList), "");
                            
                            resultCode = map.get(MarzConstant.JSON_TAG_RESCODE).getInt(MarzConstant.JSON_TAG_RESCODE);
                            
                            if (MarzConstant.RES_CODE_SUCCESS_0 == resultCode)
                            {
                                sid = map.get(MarzConstant.JSON_TAG_SID).getString(MarzConstant.JSON_TAG_SID);
                                
                                this.marzLogService.marzLog(account, MarzConstant.MARZ_LOG_TYPE_4, "主卡 " + MarzUtil.getFaceImageUrl(baseFameCard) + " 名声合成 " + MarzUtil.getFaceImageUrlByList(fameFusionCardList));
                                
                                cardList.removeAll(fameFusionCardList);
                            }
                		}
                	}
                }
                
                // 狗粮合成
                if (validateSetting(MarzConstant.VALIDATE_SETTING_CHIARIFUSION))
                {
                    CardEvt baseCard = null;
                    
                    for (CardEvt card : cardList)
                    {
                        // 自动合成只支持SR UR跟MR 而且必须手动锁上 先找MR 不锁也可
                        // update by ken 20150503 MR不锁改为不自动喂卡
                        if (card.getLv_max() >= 60 && card.getLv() < card.getLv_max()
                                && 0 != card.getIs_lock())
                        {
                            baseCard = card;
                        }
                    }
                    // 如果没有MR以上的卡 再挑UR喂
                    if (null == baseCard)
                    {
                        for (CardEvt card : cardList)
                        {
                            // 没有MR可喂的时候，找锁上的UR喂
                            if (card.getLv_max() >= 50 && card.getLv() < card.getLv_max()
                                && 0 != card.getIs_lock())
                            {
                                baseCard = card;
                            }
                        }
                    }
                    // 如果没有UR以上的卡 再挑SR喂
                    if (null == baseCard)
                    {
                        for (CardEvt card : cardList)
                        {
                            // 没有MR UR可喂的时候，找锁上的SR喂
                            if (card.getLv_max() >= 40 && card.getLv() < card.getLv_max()
                                        && 0 != card.getIs_lock())
                            {
                            	baseCard = card;
                            }
                        }
                    }
                    
                    // 到此 baseCard已经确认，接下来看是否有狗粮可以喂
                    // 3.2.0版本之后，狗粮也被合并到材料中，只显示个数，不显示单张卡
                    String[] chiaris = {"20000002", "20000003", "20000004"};
                    List<String> chiariFusionIdList = new ArrayList<String>();
                    for (String id :  Arrays.asList(chiaris))
                    {
                        if (chiariMap.get(id) > 0)
                        {
                            chiariFusionIdList.add(id);
                        }
                    }
                    
                    if (null != baseCard && chiariFusionIdList.size() > 0)
                    {
                        map = request.cardFusion(sid, baseCard.getUniqid(), "", MarzUtil.listToString(chiariFusionIdList));
                        
                        resultCode = map.get(MarzConstant.JSON_TAG_RESCODE).getInt(MarzConstant.JSON_TAG_RESCODE);
                        
                        if (MarzConstant.RES_CODE_SUCCESS_0 == resultCode)
                        {
                            sid = map.get(MarzConstant.JSON_TAG_SID).getString(MarzConstant.JSON_TAG_SID);
                            
                            account.setCardNum(map.get(MarzConstant.JSON_TAG_CARDFUSION).getInt("card_num"));
                            account.setGold(map.get(MarzConstant.JSON_TAG_CARDFUSION).getInt("gold"));
                            
                            this.marzLogService.marzLog(account, MarzConstant.MARZ_LOG_TYPE_4, "主卡 " + MarzUtil.getFaceImageUrl(baseCard) + " 消耗狗粮 " + MarzUtil.getFaceImageUrlByIdList(chiariFusionIdList));
                        }
                    }
                }
            }
        }
        catch (Exception e)
        {
            return MarzConstant.FAILED;
        }
        
        return resultCode;
    }
    
    private int battle()
    {
        if (!validateSetting(MarzConstant.VALIDATE_SETTING_BATTLE) || account.getBp() <= 5)
        {
            return MarzConstant.SUCCESS;
        }
        
        try
        {
            // 先查询单人战斗信息
            map = request.teamBattleSoloShow(sid, this.arthurType);
            resultCode = map.get(MarzConstant.JSON_TAG_RESCODE).getInt(MarzConstant.JSON_TAG_RESCODE);
            
            //this.marzLogService.marzLog(account, MarzConstant.MARZ_LOG_TYPE_1, "战斗信息查询" + MarzUtil.resultCodeStr(resultCode));
            
            if (MarzConstant.RES_CODE_SUCCESS_0 == resultCode)
            {
                sid = map.get(MarzConstant.JSON_TAG_SID).getString(MarzConstant.JSON_TAG_SID);
                
                List<BossEvt> battleMapList = new ArrayList<BossEvt>();
                List<BossEvt> normalMapList = new ArrayList<BossEvt>();
                List<BossEvt> eventMapList = new ArrayList<BossEvt>();
                BossEvt bossEvt = new BossEvt();
                
                // 是否只战斗一次
                boolean isBattleOnce = true;
                
                JSONObject groupJSON;
                JSONArray bossArray;
                JSONObject bossJSON;
                
                String arthur1 = "";
                String arthur2 = "";
                String arthur3 = "";
                String arthur4 = "";
                
                JSONArray battleMapNormal = map.get(MarzConstant.JSON_TAG_TEAMBATTLESOLOSHOW).getJSONArray("normal_groups");                
                JSONArray battleMapEvent = map.get(MarzConstant.JSON_TAG_TEAMBATTLESOLOSHOW).getJSONArray("event_groups");
                
                // 整理当前可以战斗的BOSSID Normal
                for (int i = 0; i < battleMapNormal.size(); i++)
                {
                    groupJSON = JSONObject.fromObject(battleMapNormal.get(i));
                    
                    if (groupJSON.containsKey("bosses") && !CommonUtil.isEmpty(groupJSON.getString("bosses")))
                    {
                        bossArray = groupJSON.getJSONArray("bosses");
                        
                        for (int j = 0; j < bossArray.size(); j++)
                        {
                            bossJSON = JSONObject.fromObject(bossArray.get(j));
                            
                            bossEvt = new BossEvt(bossJSON);
                            bossEvt.setBossName(groupJSON.getString("name") + " " + bossJSON.getString("difficulty"));
                            bossEvt.setProcess(MarzConstant.MARZMAP_PROCESS_3);
                            bossEvt.setTarget(0);
                            
                            battleMapList.add(bossEvt);
                            normalMapList.add(bossEvt);
                        }
                    }
                }
                
                // 整理当前可以战斗的BOSSID Event
                for (int i = 0; i < battleMapEvent.size(); i++)
                {
                    groupJSON = JSONObject.fromObject(battleMapEvent.get(i));
                    
                    if (groupJSON.containsKey("bosses") && !CommonUtil.isEmpty(groupJSON.getString("bosses")))
                    {
                        // 副本组信息
                        String appear_end = groupJSON.getString("appear_end");
                        // 0 普通本；2 材料本；9 单BOSS本；10 SOLO本
                        String stage_type = groupJSON.getString("stage_type");
                        
                        // 该组下所有地图信息
                        bossArray = groupJSON.getJSONArray("bosses");
                        
                        // 开始处理副本分组内每个难度的BOSSID
                        for (int j = 0; j < bossArray.size(); j++)
                        {
                            bossJSON = JSONObject.fromObject(bossArray.get(j));
                            
                            bossEvt = new BossEvt(bossJSON);
                            bossEvt.setBossName((groupJSON.getString("name").contains("グループ") ? "[鍵]" : groupJSON.getString("name")) +  bossJSON.getString("difficulty"));
                            bossEvt.setAppear_end(appear_end);
                            bossEvt.setStage_type(stage_type);
                            
                            // 狗粮本跟每日限定要塞成0
                            if (bossEvt.getBossId().startsWith(MarConstant.BOSSID_HEAD_PROCESS3_CHIARI)
                                    || bossEvt.getBossId().startsWith(MarConstant.BOSSID_HEAD_PROCESS3_DAILY)
                                    || bossEvt.getBossId().startsWith(MarConstant.BOSSID_HEAD_PROCESS3_CHIARI_KEY)
                                    || bossEvt.getBossId().startsWith(MarConstant.BOSSID_HEAD_PROCESS3_DAILY_KEY))
                            {
                                bossEvt.setProcess(MarzConstant.MARZMAP_PROCESS_3);
                                bossEvt.setTarget(0);
                            }
                            else if ("10".equals(stage_type))
                            {
                                if (bossEvt.getHint().contains("３連戦"))
                                {
                                    bossEvt.setProcess(MarzConstant.MARZMAP_PROCESS_3);
                                    bossEvt.setTarget(4);
                                }
                                else if (bossEvt.getHint().contains("２連戦"))
                                {
                                    bossEvt.setProcess(MarzConstant.MARZMAP_PROCESS_2);
                                    bossEvt.setTarget(4);
                                }
                                else
                                {
                                    bossEvt.setProcess(MarzConstant.MARZMAP_PROCESS_1);
                                    bossEvt.setTarget(4);
                                }
                            }
                            else
                            {
                                bossEvt.setProcess(MarzConstant.MARZMAP_PROCESS_1);
                                bossEvt.setTarget(4);
                            }
                            
                            // 钥匙类型
                            if (bossEvt.getBossId().startsWith(MarConstant.BOSSID_HEAD_PROCESS3_CHIARI_KEY))
                            {
                                bossEvt.setOpenKeyType(MarConstant.ITEM_ID_KEY_CHIARI);
                            }
                            else if (bossEvt.getBossId().startsWith(MarConstant.BOSSID_HEAD_PROCESS3_DAILY_KEY))
                            {
                                bossEvt.setOpenKeyType(MarConstant.ITEM_ID_KEY_KIRARI);
                            }
                            else if (bossEvt.getBossId().startsWith(MarConstant.BOSSID_HEAD_PROCESS1_DRAGON_KEY)
                                    || bossEvt.getBossId().startsWith(MarConstant.BOSSID_HEAD_PROCESS1_EVENT_KEY))
                            {
                                bossEvt.setOpenKeyType(MarConstant.ITEM_ID_KEY_BOSS);
                            }
                            else
                            {
                                bossEvt.setOpenKeyType("0");
                            }
                            
                            // 展示顺序 以及VIP
                            if (bossEvt.getBossId().startsWith("8"))
                            {
                                bossEvt.setSort("0");
                                bossEvt.setVip(MarzConstant.MARZ_ACCOUNT_VIP_2);
                            }
                            else if (bossEvt.getBossId().startsWith(MarConstant.BOSSID_HEAD_PROCESS3_CHIARI))
                            {
                                bossEvt.setSort("99");
                                bossEvt.setVip(MarzConstant.MARZ_ACCOUNT_VIP_1);
                            }
                            else if (bossEvt.getDifficulty().contains("超弩級"))
                            {
                                bossEvt.setSort("98");
                                bossEvt.setVip(MarzConstant.MARZ_ACCOUNT_VIP_1);
                            }
                            else if (!Util.isEmpty(bossEvt.getUser_buff_id()))
                            {
                                bossEvt.setSort("9");
                                bossEvt.setVip(MarzConstant.MARZ_ACCOUNT_VIP_1);
                            }
                            else
                            {
                                bossEvt.setSort("80");
                                bossEvt.setVip(MarzConstant.MARZ_ACCOUNT_VIP_1);
                            }
                            
                            // 校验是否需要添加入库
                            MarzDataPool.getInstance().addMarzMap(new MarzMapEvt(bossEvt));
                            
                            battleMapList.add(bossEvt);
                            eventMapList.add(bossEvt);
                        }
                    }
                }
                // 至此全部地图都已经处理完毕
                
                // 先初始化地图对象 后续检查是否被赋值，然后再看是否需要走一般流程
                bossEvt = new BossEvt();
                
                // 先看是否开启了练级模式
                if (validateSetting(MarzConstant.VALIDATE_SETTING_BATTLE_LEVEL_UP))
                {
                    boolean isAllNormalComplete = false;
                    
                    // 先看是否已经打完所有图了
                    for (BossEvt map : normalMapList)
                    {
                        if (MarConstant.BATTLESOLOSTART_18_1.equals(map.getBossId()) && MarzConstant.MARZMAP_STATE_2.equals(map.getState()))
                        {
                            isAllNormalComplete = true;
                            break;
                        }
                    }
                    
                    if (isAllNormalComplete)
                    {
                        for (BossEvt map : normalMapList)
                        {
                            if (MarConstant.BATTLESOLOSTART_17_3.equals(map.getBossId()))
                            {
                                bossEvt = map;
                                break;
                            }
                        }
                    }
                    
                    // 如果还没有全部打完 则继续最新的
                    if (CommonUtil.isEmpty(bossEvt.getBossId()))
                    {
                        for (BossEvt map : normalMapList)
                        {
                            // 如果地图的标志为标明未通过 并且体力够打这个图 那么就打这个
                            if ((MarzConstant.MARZMAP_STATE_0.equals(map.getState()) || MarzConstant.MARZMAP_STATE_1.equals(map.getState()))
                                    && account.getBpMax() >= map.getBpCost())
                            {
                                bossEvt = map;
                                break;
                            }
                        }
                    }
                }
                
                // 如果开启了优先拿石模式 则先找没拿过石头的副本 拿石模式下不包含8开头的带锁副本
                if (CommonUtil.isEmpty(bossEvt.getBossId()) && validateSetting(MarzConstant.VALIDATE_SETTING_BATTLE_GET_STONE))
                {
                    // 拿石模式只处理Event副本
                    for (BossEvt map : eventMapList)
                    {
                        // 过滤8开头的副本 打了也没有石头
                        if (map.getBossId().startsWith("8"))
                        {
                            continue;
                        }
                        
                        // 如果地图的标志为标明未通过 并且体力够打这个图 那么就打这个
                        if ((MarzConstant.MARZMAP_STATE_0.equals(map.getState()) || MarzConstant.MARZMAP_STATE_1.equals(map.getState()))
                                && account.getBpMax() >= map.getBpCost())
                        {
                            if (!"-1".equals(map.getAppear_end()))
                            {
                                bossEvt = map;
                                break;
                            }
                        }
                    }
                }
                
                // 这里判断上面的是否选中了需要打的图 如果没有 则走用户自定义流程
                if (CommonUtil.isEmpty(bossEvt.getBossId()))
                {
                    // 判断应该打哪张图
                    List<String> userMapList = MarzUtil.stringToList(account.getBossIds());
                    bossEvt = new BossEvt();
                    
                    for (String id : userMapList)
                    {
                        for (BossEvt m : battleMapList)
                        {
                            //if (id.equals(m.getBossId()) && account.getBpMax() >= m.getBpCost())
                            // update by ken 20150907 for battle id to id1-id2-id3
                            if (id.contains(m.getBossId()) && account.getBpMax() >= m.getBpCost())
                            {
                                bossEvt = m;
                                /*
                                MarzMapEvt map = MarzDataPool.getInstance().getMarzMapByBossId(m.getBossId());
                                if (null != map)
                                {
                                    //bossEvt.setProcess(map.getProcess());
                                    bossEvt.setTarget(map.getTarget());
                                    //mapEvt.setEnemy(mapList.get(0).getEnemy());
                                }
                                */
                                break;
                            }
                        }
                        
                        if (!CommonUtil.isEmpty(bossEvt.getBossId()))
                        {
                            // 自定义副本可以循环打
                            isBattleOnce = false;
                            break;
                        }
                    }
                }
                
                // 没有可以打的副本时
                if (CommonUtil.isEmpty(bossEvt.getBossId()))
                {
                    // 是否启用不浪费BP
                    if (validateSetting(MarzConstant.VALIDATE_SETTING_BATTLE_NOWASTE) && account.getBp() >= account.getBpMax())
                    {
                        // 不浪费BP打的地图为空或者为日限轮询的0时
                        if (CommonUtil.isEmpty(marzSettingEvt.getBattleNowasteBossId()) || "0".equals(marzSettingEvt.getBattleNowasteBossId()))
                        {
                            for (BossEvt m : battleMapList)
                            {
                                if (MarConstant.BATTLE_START_MONDAY.equals(m.getBossId()) && account.getBp() >= m.getBpCost())
                                {
                                    bossEvt = m;
                                }
                                else if (MarConstant.BATTLE_START_TUESDAY.equals(m.getBossId()) && account.getBp() >= m.getBpCost())
                                {
                                    bossEvt = m;
                                }
                                else if (MarConstant.BATTLE_START_WEDNESDAY.equals(m.getBossId()) && account.getBp() >= m.getBpCost())
                                {
                                    bossEvt = m;
                                }
                                else if (MarConstant.BATTLE_START_THURSDAY.equals(m.getBossId()) && account.getBp() >= m.getBpCost())
                                {
                                    bossEvt = m;
                                }
                                else if (MarConstant.BATTLE_START_FRIDAY.equals(m.getBossId()) && account.getBp() >= m.getBpCost())
                                {
                                    bossEvt = m;
                                }
                                else if (MarConstant.BATTLE_START_SATURDAY.equals(m.getBossId()) && account.getBp() >= m.getBpCost())
                                {
                                    bossEvt = m;
                                }
                                else if (MarConstant.BATTLE_START_SUNDAY.equals(m.getBossId()) && account.getBp() >= m.getBpCost())
                                {
                                    bossEvt = m;
                                }
                            }
                        }
                        else
                        {
                            for (BossEvt m : battleMapList)
                            {
                                //if (marzSettingEvt.getBattleNowasteBossId().equals(m.getBossId()) && account.getBp() >= m.getBpCost())
                                // update by ken 20150907 for battle id to id1-id2-id3
                                if (marzSettingEvt.getBattleNowasteBossId().contains(m.getBossId()) && account.getBp() >= m.getBpCost())
                                {
                                    bossEvt = m;
                                }
                            }
                        }
                    }
                    else
                    {
                        return MarzConstant.SUCCESS;
                    }
                }
                
                // 校验数据
                if  (!CommonUtil.isEmpty(bossEvt.getBossId()))
                {
                    
                    if (itemUseFlag && account.getBp() < bossEvt.getBpCost())
                    {
                        // 有药水了可以直接嗑药
                        map = request.itemUse(sid, this.itemUseId);
                        
                        resultCode = map.get(MarzConstant.JSON_TAG_RESCODE).getInt(MarzConstant.JSON_TAG_RESCODE);
                        
                        if (MarzConstant.RES_CODE_SUCCESS_0 == resultCode)
                        {
                            sid = map.get(MarzConstant.JSON_TAG_SID).getString(MarzConstant.JSON_TAG_SID);
                            this.marzLogService.marzLog(account, MarzConstant.MARZ_LOG_TYPE_3, "药水使用成功！");
                            account.setBp(account.getBp() + account.getBpMax());
                            //JSONObject user = map.get(MarzConstant.JSON_TAG_ITEMUSE);
                            //account.setBp(user.getJSONObject("user").getInt("bp"));
                            //this.saveAccount();
                            //this.marzLogService.marzLog(account, MarzConstant.MARZ_LOG_TYPE_3, "药水使用成功！BP变为：" + user.getJSONObject("user").getInt("bp"));
                        }
                    }
                    
                    // update by ken 20160805 如果为钥匙本，则需要先自动使用钥匙开一次门  如果不够打两次，还是不开门吧
                    if (Util.isNotEmpty(bossEvt.getUser_buff_id()) && account.getBp() > (bossEvt.getBpCost() * 2))
                    {
                        try
                        {
                            // 这里开门的动作报错也不做任何处理
                            // 如果开门失败则后面会自动退出
                            for (String ubId : bossEvt.getUser_buff_id())
                            {
                                map = request.userBuffExec(sid, ubId);
                                
                                if (MarzConstant.RES_CODE_SUCCESS_0 == resultCode)
                                {
                                    sid = map.get(MarzConstant.JSON_TAG_SID).getString(MarzConstant.JSON_TAG_SID);
                                    this.marzLogService.marzLog(account, MarzConstant.MARZ_LOG_TYPE_1, "使用钥匙开启副本！");
                                    break;
                                }
                            }
                        }
                        catch (Exception e)
                        {
                            // 不做任何处理
                        }
                    }
                    
                	// update by ken 20150621 战斗改为一次性循环打完所有BP
                    do
                    {
                        if (Thread.currentThread().getName().contains(MarzConstant.OVER))
                        {
                            break;
                        }
                        
                        if (account.getBp() < bossEvt.getBpCost())
                        {
                            this.marzLogService.marzLog(account, MarzConstant.MARZ_LOG_TYPE_1, "体力不足，停止战斗！目标副本：" + bossEvt.getBossName() + " 当前体力：" + account.getBp() + " 副本需要：" + bossEvt.getBpCost());
                            break;
                        }
                        
                        // add by ken 20150810 for solo map
                        if (MarConstant.STAGE_TYPE_10.equals(bossEvt.getStage_type()))
                        {
                            arthur1 = account.getUserId();
                            arthur2 = account.getUserId();
                            arthur3 = account.getUserId();
                            arthur4 = account.getUserId();
                            
                            if (bossEvt.getBossName().startsWith("傭兵"))
                            {
                                arthurType = "1";
                            }
                            else if (bossEvt.getBossName().startsWith("富豪"))
                            {
                                arthurType = "2";
                            }
                            else if (bossEvt.getBossName().startsWith("盗賊"))
                            {
                                arthurType = "3";
                            }
                            else if (bossEvt.getBossName().startsWith("歌姫"))
                            {
                                arthurType = "4";
                            }
                            else
                            {
                                arthurType = "1";
                            }
                        }
                        
                        // add by Ken 20161019 for v4.6.1 添加对is_only_my_deck的判断
                        if ("0".equals(bossEvt.getIs_only_my_deck()))
                        {
                            // add by Ken 20161009 for v4.6.0 arthurs信息单独开了一个新的接口TeamBattleSoloPartnerShow
                            map = request.teamBattleSoloPartnerShow(sid, bossEvt.getBossId());
                            
                            JSONArray arthurs = map.get(MarzConstant.JSON_TAG_TEAMBATTLESOLOPARTNERSHOW).getJSONArray("arthurs");
                            JSONObject arthur;
                            
                            // 处理4个NPC亚瑟的ID
                            for (int i = 0; i < arthurs.size(); i++)
                            {
                                arthur = JSONObject.fromObject(arthurs.get(i));
                                
                                if (1 == arthur.getInt("arthur_type"))
                                {
                                    arthur1 = JSONObject.fromObject(arthur.getJSONArray("partners").get(0)).getString("userid");
                                }
                                else if (2 == arthur.getInt("arthur_type"))
                                {
                                    arthur2 = JSONObject.fromObject(arthur.getJSONArray("partners").get(0)).getString("userid");
                                }
                                else if (3 == arthur.getInt("arthur_type"))
                                {
                                    arthur3 = JSONObject.fromObject(arthur.getJSONArray("partners").get(0)).getString("userid");
                                }
                                else if (4 == arthur.getInt("arthur_type"))
                                {
                                    arthur4 = JSONObject.fromObject(arthur.getJSONArray("partners").get(0)).getString("userid");
                                }
                            }
                        }
                        else if ("1".equals(bossEvt.getIs_only_my_deck()))
                        {
                            // 该类型副本只能自己的卡组打
                            arthur1 = account.getUserId();
                            arthur2 = account.getUserId();
                            arthur3 = account.getUserId();
                            arthur4 = account.getUserId();
                        }
                        
                    	map = request.teamBattleSoloStart(sid, bossEvt.getBossId(), arthurType, arthur1, arthur2, arthur3, arthur4);
                        
                        resultCode = map.get(MarzConstant.JSON_TAG_RESCODE).getInt(MarzConstant.JSON_TAG_RESCODE);
                        
                        this.marzLogService.marzLog(account, MarzConstant.MARZ_LOG_TYPE_1, "战斗开始" + MarzUtil.resultCodeStr(resultCode) + " 目标副本 " + bossEvt.getBossName());
                        
                        if (MarzConstant.RES_CODE_SUCCESS_0 == resultCode)
                        {
                            sid = map.get(MarzConstant.JSON_TAG_SID).getString(MarzConstant.JSON_TAG_SID);
                            account.setBp(account.getBp() - bossEvt.getBpCost());
                            
                            String battleEndParam = "";
                            
                            // update by ken 2016-4-26 for 手动更新process防止某些副本卡死无法完成
                            // 从数据库中查询出该副本，回写process值
                            MarzMapEvt mapEvt = MarzDataPool.getInstance().getMarzMapByBossId(bossEvt.getBossId());
                            
                            if (Util.isNotEmpty(mapEvt) && Util.isNotEmpty(mapEvt.getProcess()))
                            {
                                bossEvt.setProcess(mapEvt.getProcess());
                            }
                            
                            // update by ken 2016-1-25 for 当前改为只通过process判断结果参数
                            //if (0 == bossEvt.getTarget() && MarzConstant.MARZMAP_PROCESS_3 == bossEvt.getProcess())
                            if (MarzConstant.MARZMAP_PROCESS_3 == bossEvt.getProcess())
                            {
                                battleEndParam = MarConstant.BATTLESOLOEND_3;
                            }
                            //else if (1 == bossEvt.getTarget() && MarzConstant.MARZMAP_PROCESS_1 == bossEvt.getProcess())
                            else if (MarzConstant.MARZMAP_PROCESS_2 == bossEvt.getProcess())
                            {
                                battleEndParam = MarConstant.BATTLESOLOEND_2;
                            }
                            else
                            {
                                battleEndParam = MarConstant.BATTLESOLOEND_1;
                                /* 2016-1-25 取消等级制度
                                if (MarzConstant.MARZ_ACCOUNT_VIP_0.equals(account.getVip()))
                                {
                                    battleEndParam = MarConstant.BATTLESOLOEND_1_2;
                                }
                                else if (MarzConstant.MARZ_ACCOUNT_VIP_1.equals(account.getVip()))
                                {
                                    battleEndParam = MarConstant.BATTLESOLOEND_1_3;
                                }
                                else if (MarzConstant.MARZ_ACCOUNT_VIP_2.equals(account.getVip()))
                                {
                                    battleEndParam = MarConstant.BATTLESOLOEND_1_4;
                                }
                                else if (MarzConstant.MARZ_ACCOUNT_VIP_3.equals(account.getVip()))
                                {
                                    battleEndParam = MarConstant.BATTLESOLOEND_1_5;
                                }
                                */
                            }
                            
                            Thread.sleep(MarzConstant.SLEEPTIME_BATTLE_SOLO);
                            
                            // 4.4.0版本开始添加bossId字段
                            map = request.teamBattleSoloEnd(sid, battleEndParam + bossEvt.getBossId() + "}");
                            
                            resultCode = map.get(MarzConstant.JSON_TAG_RESCODE).getInt(MarzConstant.JSON_TAG_RESCODE);
                            List<String> newCardIdList = new ArrayList<String>();
                            
                            if (MarzConstant.RES_CODE_SUCCESS_0 == resultCode)
                            {
                                sid = map.get(MarzConstant.JSON_TAG_SID).getString(MarzConstant.JSON_TAG_SID);
                                
                                JSONObject user = map.get(MarzConstant.JSON_TAG_TEAMBATTLESOLOEND).getJSONObject("user");
                                
                                this.jsonUser(account, user);
                                this.saveAccount();
                                
                                JSONArray result_rewards = map.get(MarzConstant.JSON_TAG_TEAMBATTLESOLOEND).getJSONArray("result_rewards");
                                JSONObject reward;
                                
                                if (null != result_rewards)
                                {
                                    for (int i = 0; i < result_rewards.size(); i++)
                                    {
                                        reward = result_rewards.getJSONObject(i);
                                        if (reward.getJSONObject("reward").getInt("type") == 6
                                                || reward.getJSONObject("reward").getInt("type") == 13)
                                        {
                                            newCardIdList.add(reward.getJSONObject("reward").getString("reward_typeid"));
                                        }
                                    }
                                }
                            }
                            
                            this.marzLogService.marzLog(account, MarzConstant.MARZ_LOG_TYPE_1, "战斗结束" + MarzUtil.resultCodeStr(resultCode) + " 目标副本 " + bossEvt.getBossName() + " 战斗获得 " + MarzUtil.getFaceImageUrlByIdList(newCardIdList));
                            
                            if (isBattleOnce)
                            {
                                // 自动过普通副本 优先石头 不浪费BP 每轮任务只打一次
                                break;
                            }
                        }
                        else
                        {
                            // 战斗失败 跳出
                            break;
                        }
                    }
                    while (MarzConstant.RES_CODE_SUCCESS_0 == resultCode
                            && account.getBp() >= bossEvt.getBpCost()
                            //&& !marzSettingEvt.getBattleNowasteBossId().equals(mapEvt.getBossId()) // 如果这个图是不浪费BP刷的图 则只刷一次
                            //&& !(CommonUtil.isEmpty(marzSettingEvt.getBattleNowasteBossId()) || "0".equals(marzSettingEvt.getBattleNowasteBossId())) // 如果选择7合1也要过滤
                    		&& MarzConstant.MARZMAP_STATE_2.equals(bossEvt.getState())); // 练级模式以及拿石模式下 第一次打的图不重复刷
                }
                else
                {
                    this.marzLogService.marzLog(account, MarzConstant.MARZ_LOG_TYPE_1, account.getTgksId() + "当前时间没有找到适合战斗的副本，返回并等待BP恢复！");
                }
            }
        }
        catch (Exception e)
        {
            CommonUtil.infoLog(logger, CommonConstant.SYSTEM_INFO_LOG_METHOD_OUT, String.format(account.getTgksId() + "战斗过程中发生异常！"));
            System.out.println(e.getMessage());
            return MarzConstant.FAILED;
        }
        
        return resultCode;
    }
    
    private int pvp()
    {
        // true表示PVP打完不需要再发请求
        if (this.pvpChallengeOverFlag)
        {
            return MarzConstant.SUCCESS;
        }
        
        // add by ken 20150907 for pvp time limit
        /*
        if (!MarzUtil.inPvpTime())
        {
            return MarzConstant.SUCCESS;
        }
        */
        
        if (!validateSetting(MarzConstant.VALIDATE_SETTING_PVP))
        {
            return MarzConstant.SUCCESS;
        }
        
        if (this.deckMap.size() < 4)
        {
            this.marzLogService.marzLog(account, MarzConstant.MARZ_LOG_TYPE_6, "四职业卡组没有配齐，请先设定好四职业卡组...");
            return MarzConstant.SUCCESS;
        }
        
        try
        {
            // 先调用pvpShow
            map = request.pvpShow(sid);
            resultCode = map.get(MarzConstant.JSON_TAG_RESCODE).getInt(MarzConstant.JSON_TAG_RESCODE);
            
            if (MarzConstant.RES_CODE_SUCCESS_0 == resultCode)
            {
                sid = map.get(MarzConstant.JSON_TAG_SID).getString(MarzConstant.JSON_TAG_SID);
                
                // 看剩余次数是否>0
                JSONObject pvpShow = map.get(MarzConstant.JSON_TAG_PVPSHOW);
                
                if (pvpShow.getInt("challenge") < 1)
                {
                    this.pvpChallengeOverFlag = true;
                    this.marzLogService.marzLog(account, MarzConstant.MARZ_LOG_TYPE_6, "本日PVP结算场次已经用完！停止相关PVP行动...");
                    // 次数小于1时直接返回
                    return MarzConstant.SUCCESS;
                }
                
                // 这里选出PVP的主职业
                // change by Ken 20161116 for v4.7.0
                String arthurPvpType = "1";
                map = this.request.pvpStart2(sid, MarzConstant.MARZPVP_TYPE_1, arthurPvpType, deckMap);
                resultCode = map.get(MarzConstant.JSON_TAG_RESCODE).getInt(MarzConstant.JSON_TAG_RESCODE);
                
                if (MarzConstant.RES_CODE_SUCCESS_0 == resultCode)
                {
                    sid = map.get(MarzConstant.JSON_TAG_SID).getString(MarzConstant.JSON_TAG_SID);
                    
                    JSONObject pvpStart = map.get(MarzConstant.JSON_TAG_PVPSTART);
                    // 这个是用来发送结束报文用的 很重要
                    //String btluid = pvpStart.getString("btluid");
                    String enemy = pvpStart.getJSONObject("enemy_info").getString("name");
                    int bf_pvp_point = pvpStart.getInt("bf_pvp_point");
                    int af_pvp_point = pvpStart.getInt("af_pvp_point");
                    int challenge = pvpStart.getInt("challenge");
                    
                    this.marzLogService.marzLog(account, MarzConstant.MARZ_LOG_TYPE_6, "斗技场PVP战斗对手为：" + enemy + "战斗结果：" + (af_pvp_point > bf_pvp_point ? "胜利" : "失败") + "，本日剩余场次：" + challenge);
                    /*
                    Thread.sleep(MarzConstant.SLEEPTIME_BATTLE_SOLO);
                    
                    map = this.request.pvpEnd(sid, btluid, pvpEndMap.get(arthurPvpType));
                    resultCode = map.get(MarzConstant.JSON_TAG_RESCODE).getInt(MarzConstant.JSON_TAG_RESCODE);
                    
                    if (MarzConstant.RES_CODE_SUCCESS_0 == resultCode)
                    {
                        this.marzLogService.marzLog(account, MarzConstant.MARZ_LOG_TYPE_6, "斗技场PVP战斗结束，战斗结果：胜利");
                        
                        sid = map.get(MarzConstant.JSON_TAG_SID).getString(MarzConstant.JSON_TAG_SID);
                    }
                    */
                }
                
            }
        }
        catch (Exception e)
        {
            this.marzLogService.marzLog(account, MarzConstant.MARZ_LOG_TYPE_0, "PVP信息查询失败...");
        }
        
        return resultCode;
    }
    
    /**
     * 
     * @Title: validateSetting
     * @Description: 校验开关是否开启
     * @param settingTag
     * @return
     * @return boolean 返回类型
     * @throws
     */
    private boolean validateSetting(int settingTag)
    {
        switch (settingTag)
        {
            case MarzConstant.VALIDATE_SETTING_EXPLORE: // 自动跑图开关
                return MarzConstant.MARZSETTING_ON.equals(marzSettingEvt.getExplore());
            case MarzConstant.VALIDATE_SETTING_CARDSELL: // 自动卖卡开关
                return MarzConstant.MARZSETTING_ON.equals(marzSettingEvt.getCardSell());
            case MarzConstant.VALIDATE_SETTING_CARDSELL_COMMON: // 自动卖卡开关-卖普通卡
                return MarzConstant.MARZSETTING_ON.equals(marzSettingEvt.getCardSellCommon());
            case MarzConstant.VALIDATE_SETTING_CHIARIFUSION: // 自动喂狗粮开关
                return MarzConstant.MARZSETTING_ON.equals(marzSettingEvt.getCardFusion());
            case MarzConstant.VALIDATE_SETTING_BATTLE: // 自动战斗开关
                return MarzConstant.MARZSETTING_ON.equals(marzSettingEvt.getBattle());
            case MarzConstant.VALIDATE_SETTING_BATTLE_NOWASTE: // 自动战斗开关-不浪费BP
                return MarzConstant.MARZSETTING_ON.equals(marzSettingEvt.getBattleNowaste());
            case MarzConstant.VALIDATE_SETTING_BATTLE_GET_STONE: // 优先拿石开关
                return MarzConstant.MARZSETTING_ON.equals(marzSettingEvt.getBattleGetStone());
            case MarzConstant.VALIDATE_SETTING_PVP: // PVP开关
                return MarzConstant.MARZSETTING_ON.equals(marzSettingEvt.getPvp());
            case MarzConstant.VALIDATE_SETTING_FAMEFUSION: // 自动名声合成开关
                return MarzConstant.MARZSETTING_ON.equals(marzSettingEvt.getFameFusion());
            case MarzConstant.VALIDATE_SETTING_COINGACHA: // 抽硬币
                return MarzConstant.MARZSETTING_ON.equals(marzSettingEvt.getCoinGacha());
            case MarzConstant.VALIDATE_SETTING_AUTOUSEBPPOTION: // 自动喝药
                return MarzConstant.MARZSETTING_ON.equals(marzSettingEvt.getAutoUseBPPotion());
            case MarzConstant.VALIDATE_SETTING_AUTOBUYBPPOTION: // 自动买药
                return MarzConstant.MARZSETTING_ON.equals(marzSettingEvt.getAutoBuyBPPotion());
            case MarzConstant.VALIDATE_SETTING_CARDSELL_EVO: // 出售进化素材
                return MarzConstant.MARZSETTING_ON.equals(marzSettingEvt.getCardSellEvo());
            case MarzConstant.VALIDATE_SETTING_BATTLE_LEVEL_UP: // 练级模式
                return MarzConstant.MARZSETTING_ON.equals(marzSettingEvt.getBattleLevelUp());
            case MarzConstant.VALIDATE_SETTING_AUTOPRESENTRECV: // 自动拿出带期限的礼物
                return MarzConstant.MARZSETTING_ON.equals(marzSettingEvt.getAutoPresentRecv());
            default:
                return false;
        }
    }
    
    /**
     * 
     * @Title: jsonUser
     * @Description: 根据JSON更新account数据
     * @param account
     * @param user
     * @return void 返回类型
     * @throws
     */
    private void jsonUser(MarzAccountEvt account, JSONObject user)
    {
        account.setAp(user.getInt("ap"));
        account.setApMax(user.getInt("ap_max"));
        account.setBp(user.getInt("bp"));
        account.setBpMax(user.getInt("bp_max"));
        account.setCardMax(user.getInt("card_max"));
        account.setCardNum(user.getInt("card_num"));
        account.setCoin(user.getInt("coin") + user.getInt("coin_free"));
        account.setFp(user.getInt("fp"));
        account.setGold(user.getInt("gold"));
        account.setLv(user.getInt("lv"));
        account.setName(user.getString("name"));
        account.setUserId(user.getString("userid"));
    }
    
    /**
     * 
     * @Title: saveAccount
     * @Description: 保存之前先查询最新的数据 防止脏数据情况发生
     * @return void 返回类型
     * @throws
     */
    private void saveAccount() throws Exception
    {
        // 保存之前先查询最新的数据 防止脏数据情况发生
        MarzAccountEvt tempAccount = this.marzAccountService.queryMarzAccountById(account.getId());
        account.setBossIds(tempAccount.getBossIds());
        account.setSellCardIds(tempAccount.getSellCardIds());
        account.setFameCardIds(tempAccount.getFameCardIds());
        account.setStatus(tempAccount.getStatus());
        
        if (CommonUtil.isEmpty(account.getGachaHash()))
        {
            account.setGachaHash(MarzUtil.GenerateGachaHash(account.getUserId()));
        }
        
        this.marzAccountService.updateMarzAccount(account);
    }

    @Override
    public void setApplicationContext(ApplicationContext applicationContext)
            throws BeansException
    {
        ContextUtil.setApplicationContext(applicationContext);
    }

    public static Log getLogger()
    {
        return logger;
    }

    public static void setLogger(Log logger)
    {
        MarzTaskDiffusion.logger = logger;
    }
}
