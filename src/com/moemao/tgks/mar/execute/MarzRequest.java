package com.moemao.tgks.mar.execute;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import net.sf.json.JSONObject;

import com.moemao.tgks.common.tool.CommonUtil;
import com.moemao.tgks.mar.marz.entity.DeckEvt;
import com.moemao.tgks.mar.marz.tool.MarzConstant;
import com.moemao.tgks.mar.marz.tool.MarzUtil;
import com.moemao.tgks.mar.net.HttpRequest;
import com.moemao.tgks.mar.tool.MarConstant;

public class MarzRequest
{
    private MarzRequest() {}
    
    private static MarzRequest instance;
    
    private HttpRequest httpRequest = new HttpRequest();
    
    private Map<String, JSONObject> map;
    
    public static synchronized MarzRequest getInstance()
    {
        if (null == instance)
        {
            instance = new MarzRequest();
        }
        return instance;
    }
    
    private JSONObject sidJSONObject(String sid)
    {
        JSONObject json = new JSONObject();
        json.put("sid", sid);
        return json;
    }
    
    public void authCheckIOS(String uuid) throws Exception
    {
    	map = new HashMap<String, JSONObject>();
    	String paramStr = "{\"market\":1,\"os\":0,\"uuid\":\"" + uuid + "\",\"clver\":\"" + MarzUtil.getMarzDBConfig(MarzConstant.DBCONFIG_LOGIN_CLVER_IOS) + "\",\"bsid\":\"\"}";
    	httpRequest.sendPost(MarConstant.URL_AUTH_CHECK, paramStr);
    	System.out.println(MarzConstant.LOG_SYSTEM_INFO + "authCheckIOS " + uuid + " " + Thread.currentThread().getName());
    }
    
    public void authCheckAndroid(String uuid) throws Exception
    {
    	map = new HashMap<String, JSONObject>();
    	String paramStr = "{\"market\":2,\"os\":1,\"uuid\":\"" + uuid + "\",\"clver\":\"" + MarzUtil.getMarzDBConfig(MarzConstant.DBCONFIG_LOGIN_CLVER_ANDROID) + "\",\"bsid\":\"\"}";
    	httpRequest.sendPost(MarConstant.URL_AUTH_CHECK, paramStr);
    	System.out.println(MarzConstant.LOG_SYSTEM_INFO + "authCheckAndroid " + uuid + " " + Thread.currentThread().getName());
    }
    
    public Map<String, JSONObject> loginIOS(String uuid, String hashToken) throws Exception
    {
        map = new HashMap<String, JSONObject>();
        
        String paramStr = "{\"uuid\":\"" + uuid + "\",\"hash_token\":\"" + MarzUtil.GenerateHashToken(uuid) + "\",\"clver\":\"" + MarzUtil.getMarzDBConfig(MarzConstant.DBCONFIG_LOGIN_CLVER_IOS) + "\",\"os\":0,\"carrier\":3,\"market\":1,\"lang\":0,\"device\":\"iPhone6Plus\",\"token\":\"\",\"os_ver\":\"iPhone OS 9.3.2\",\"bsid\":\"\"}";
        
        String result = httpRequest.sendPost(MarConstant.URL_LOGIN, paramStr);
        System.out.println(MarzConstant.LOG_SYSTEM_INFO + "loginIOS " + uuid + " " + Thread.currentThread().getName());

        JSONObject resCode= JSONObject.fromObject(result);
        
        map.put(MarzConstant.JSON_TAG_SID, this.sidJSONObject(resCode.getString("sess_key").replace("=", "")));
        map.put(MarzConstant.JSON_TAG_RESCODE, resCode);
        
        result = null;
        resCode = null;
        return map;
    }
    
    public Map<String, JSONObject> loginAndroid(String uuid, String hashToken) throws Exception
    {
        map = new HashMap<String, JSONObject>();
        
        String paramStr = "{\"uuid\":\"" + uuid + "\",\"hash_token\":\"" + MarzUtil.GenerateHashToken(uuid) + "\",\"clver\":\"" + MarzUtil.getMarzDBConfig(MarzConstant.DBCONFIG_LOGIN_CLVER_ANDROID) + "\",\"os\":1,\"carrier\":1,\"market\":2,\"lang\":0,\"device\":\"LGE Nexus 5\",\"token\":\"\",\"os_ver\":\"Android OS 4.4.3 / API-19 (FUCK YOU)\",\"bsid\":\"\"}";
        
        String result = httpRequest.sendPost(MarConstant.URL_LOGIN, paramStr);
        System.out.println(MarzConstant.LOG_SYSTEM_INFO + "loginAndroid " + uuid + " " + Thread.currentThread().getName());

        JSONObject resCode= JSONObject.fromObject(result);
        
        map.put(MarzConstant.JSON_TAG_SID, this.sidJSONObject(resCode.getString("sess_key").replace("=", "")));
        map.put(MarzConstant.JSON_TAG_RESCODE, resCode);
        
        result = null;
        resCode = null;
        return map;
    }
    
    public Map<String, JSONObject> loginAndroidSE(String uuid, String hashToken) throws Exception
    {
        map = new HashMap<String, JSONObject>();
        
        String paramStr = "{\"uuid\":\"" + uuid + "\",\"hash_token\":\"" + MarzUtil.GenerateHashToken(uuid) + "\",\"clver\":\"" + MarzUtil.getMarzDBConfig(MarzConstant.DBCONFIG_LOGIN_CLVER_ANDROID) + "\",\"os\":1,\"carrier\":1,\"market\":3,\"lang\":0,\"device\":\"LGE Nexus 5\",\"token\":\"\",\"os_ver\":\"Android OS 4.4.3 / API-19 (FUCK YOU)\",\"bsid\":\"\"}";
        
        String result = httpRequest.sendPost(MarConstant.URL_LOGIN, paramStr);
        System.out.println(MarzConstant.LOG_SYSTEM_INFO + "loginAndroidSE " + uuid + " " + Thread.currentThread().getName());

        JSONObject resCode= JSONObject.fromObject(result);
        
        map.put(MarzConstant.JSON_TAG_SID, this.sidJSONObject(resCode.getString("sess_key").replace("=", "")));
        map.put(MarzConstant.JSON_TAG_RESCODE, resCode);
        
        result = null;
        resCode = null;
        return map;
    }
    
    public Map<String, JSONObject> inviteCodeEnter(String sid, String inviteCode) throws Exception
    {
        map = new HashMap<String, JSONObject>();
        
        String paramStr = sid + "={\"inviteid\":\"" + inviteCode + "\"}";
        String[] result = httpRequest.sendPost(MarConstant.URL_INVITECODEENTER, paramStr).split(MarConstant.KRSMA_SPLIT);
        System.out.println(MarConstant.LOG_SYSTEM_INFO + "inviteCodeEnter " + sid + " invite : " + inviteCode);
        
        JSONObject resCode= JSONObject.fromObject(result[1].substring(0, result[1].indexOf("}{") + 1));
        
        map.put(MarzConstant.JSON_TAG_SID, this.sidJSONObject(result[0]));
        map.put(MarzConstant.JSON_TAG_RESCODE, resCode);
        
        result = null;
        resCode = null;
        return map;
    }
    
    public Map<String, JSONObject> connect(String sid) throws Exception
    {
        map = new HashMap<String, JSONObject>();
        
        String paramStr = sid + "=";
        String[] result = httpRequest.sendPost(MarConstant.URL_CONNECT, paramStr).split(MarConstant.KRSMA_SPLIT);
        System.out.println(MarzConstant.LOG_SYSTEM_INFO + "connect " + Thread.currentThread().getName());
        
        JSONObject resCode= JSONObject.fromObject(result[1].substring(0, result[1].indexOf("}{") + 1));
        
        map.put(MarzConstant.JSON_TAG_SID, this.sidJSONObject(result[0]));
        map.put(MarzConstant.JSON_TAG_RESCODE, resCode);

        result = null;
        resCode = null;
        return map;
    }
    
    public Map<String, JSONObject> homeShow(String sid) throws Exception
    {
        map = new HashMap<String, JSONObject>();
        
        String paramStr = sid + "=";
        String[] result = httpRequest.sendPost(MarConstant.URL_HOMESHOW, paramStr).split(MarConstant.KRSMA_SPLIT);
        System.out.println(MarzConstant.LOG_SYSTEM_INFO + "homeShow " + Thread.currentThread().getName());

        JSONObject resCode= JSONObject.fromObject(result[1].substring(0, result[1].indexOf("}{") + 1));
        JSONObject homeShow = JSONObject.fromObject(result[1].substring(result[1].indexOf("}{") + 1, result[1].length()));
        
        map.put(MarzConstant.JSON_TAG_SID, this.sidJSONObject(result[0]));
        map.put(MarzConstant.JSON_TAG_RESCODE, resCode);
        map.put(MarzConstant.JSON_TAG_HOMWSHOW, homeShow);

        result = null;
        resCode = null;
        return map;
    }
    
    public Map<String, JSONObject> itemShow(String sid) throws Exception
    {
        map = new HashMap<String, JSONObject>();
        
        String paramStr = sid + "={\"item_type\":0}";
        String[] result = httpRequest.sendPost(MarConstant.URL_ITEMSHOW, paramStr).split(MarConstant.KRSMA_SPLIT);
        System.out.println(MarzConstant.LOG_SYSTEM_INFO + "itemShow " + Thread.currentThread().getName());

        JSONObject resCode= JSONObject.fromObject(result[1].substring(0, result[1].indexOf("}{") + 1));
        JSONObject itemShow = JSONObject.fromObject(result[1].substring(result[1].indexOf("}{") + 1, result[1].length()));
        
        map.put(MarzConstant.JSON_TAG_SID, this.sidJSONObject(result[0]));
        map.put(MarzConstant.JSON_TAG_RESCODE, resCode);
        map.put(MarzConstant.JSON_TAG_ITEMSHOW, itemShow);

        result = null;
        resCode = null;
        return map;
    }
    
    public Map<String, JSONObject> itemUse(String sid, String itemId) throws Exception
    {
        map = new HashMap<String, JSONObject>();
        
        String paramStr = sid + "={\"itemid\":" + itemId + "}";
        String[] result = httpRequest.sendPost(MarConstant.URL_ITEMUSE, paramStr).split(MarConstant.KRSMA_SPLIT);
        System.out.println(MarzConstant.LOG_SYSTEM_INFO + "itemUse " + Thread.currentThread().getName());

        JSONObject resCode= JSONObject.fromObject(result[1].substring(0, result[1].indexOf("}{") + 1));
        JSONObject itemUse = JSONObject.fromObject(result[1].substring(result[1].indexOf("}{") + 1, result[1].length()));
        
        map.put(MarzConstant.JSON_TAG_SID, this.sidJSONObject(result[0]));
        map.put(MarzConstant.JSON_TAG_RESCODE, resCode);
        map.put(MarzConstant.JSON_TAG_ITEMUSE, itemUse);

        result = null;
        resCode = null;
        return map;
    }
    
    public Map<String, JSONObject> itemShopBuy(String sid, String itemId) throws Exception
    {
        map = new HashMap<String, JSONObject>();
        
        String paramStr = sid + "={\"item_shop_lineupid\":10" + itemId + ",\"buy_num\":1}";
        String[] result = httpRequest.sendPost(MarConstant.URL_ITEMSHOPBUY, paramStr).split(MarConstant.KRSMA_SPLIT);
        System.out.println(MarzConstant.LOG_SYSTEM_INFO + "itemShopBuy " + Thread.currentThread().getName());

        JSONObject resCode= JSONObject.fromObject(result[1].substring(0, result[1].indexOf("}{") + 1));
        JSONObject itemShopBuy = JSONObject.fromObject(result[1].substring(result[1].indexOf("}{") + 1, result[1].length()));
        
        map.put(MarzConstant.JSON_TAG_SID, this.sidJSONObject(result[0]));
        map.put(MarzConstant.JSON_TAG_RESCODE, resCode);
        map.put(MarzConstant.JSON_TAG_ITEMSHOPBUY, itemShopBuy);

        result = null;
        resCode = null;
        return map;
    }
    
    public Map<String, JSONObject> exploreStart(String sid, String arthurType, String deckIdx) throws Exception
    {
        map = new HashMap<String, JSONObject>();
        
        String paramStr = sid + "={\"arthur_type\":" + arthurType + ",\"deck_idx\":" + deckIdx + "}";

        String[] result = httpRequest.sendPost(MarConstant.URL_EXPLORESTART, paramStr).split(MarConstant.KRSMA_SPLIT);
        System.out.println(MarzConstant.LOG_SYSTEM_INFO + "exploreStart " + Thread.currentThread().getName());

        JSONObject resCode= JSONObject.fromObject(result[1].substring(0, result[1].indexOf("}{") + 1));
        JSONObject exploreStart = JSONObject.fromObject(result[1].substring(result[1].indexOf("}{") + 1, result[1].length()));
        
        map.put(MarzConstant.JSON_TAG_SID, this.sidJSONObject(result[0]));
        map.put(MarzConstant.JSON_TAG_RESCODE, resCode);
        map.put(MarzConstant.JSON_TAG_EXPLORESTART, exploreStart);

        result = null;
        resCode = null;
        return map;
    }
    
    public Map<String, JSONObject> exploreEnd(String sid) throws Exception
    {
        map = new HashMap<String, JSONObject>();
        
        String paramStr = sid + "=";
        String[] result = httpRequest.sendPost(MarConstant.URL_EXPLOREEND, paramStr).split(MarConstant.KRSMA_SPLIT);
        System.out.println(MarzConstant.LOG_SYSTEM_INFO + "exploreEnd " + Thread.currentThread().getName());
        
        // 有问候语信息 必须设置过滤
        result = this.requestFilter(result);
        
        JSONObject resCode= JSONObject.fromObject(result[1].substring(0, result[1].indexOf("}{") + 1));
        JSONObject exploreEnd = JSONObject.fromObject(result[1].substring(result[1].indexOf("}{") + 1, result[1].length()));
        
        map.put(MarzConstant.JSON_TAG_SID, this.sidJSONObject(result[0]));
        map.put(MarzConstant.JSON_TAG_RESCODE, resCode);
        map.put(MarzConstant.JSON_TAG_EXPLOREEND, exploreEnd);

        result = null;
        resCode = null;
        return map;
    }
    
    public Map<String, JSONObject> cardShow(String sid) throws Exception
    {
        map = new HashMap<String, JSONObject>();
        
        String paramStr = sid + "=";
        String[] result = httpRequest.sendPost(MarConstant.URL_CARDSHOW, paramStr).split(MarConstant.KRSMA_SPLIT);
        System.out.println(MarzConstant.LOG_SYSTEM_INFO + "cardShow " + Thread.currentThread().getName());
        
        JSONObject resCode= JSONObject.fromObject(result[1].substring(0, result[1].indexOf("}{") + 1));
        JSONObject cardShow = JSONObject.fromObject(result[1].substring(result[1].indexOf("}{") + 1, result[1].length()));
        
        map.put(MarzConstant.JSON_TAG_SID, this.sidJSONObject(result[0]));
        map.put(MarzConstant.JSON_TAG_RESCODE, resCode);
        map.put(MarzConstant.JSON_TAG_CARDSHOW, cardShow);

        result = null;
        resCode = null;
        return map;
    }
    
    public Map<String, JSONObject> cardShow2(String sid) throws Exception
    {
        map = new HashMap<String, JSONObject>();
        
        String paramStr = sid + "=";
        String[] result = httpRequest.sendPost(MarConstant.URL_CARDSHOW2, paramStr).split(MarConstant.KRSMA_SPLIT);
        System.out.println(MarzConstant.LOG_SYSTEM_INFO + "cardShow2 " + Thread.currentThread().getName());
        
        JSONObject resCode= JSONObject.fromObject(result[1].substring(0, result[1].indexOf("}{") + 1));
        JSONObject cardShow = JSONObject.fromObject(result[1].substring(result[1].indexOf("}{") + 1, result[1].length()));
        
        map.put(MarzConstant.JSON_TAG_SID, this.sidJSONObject(result[0]));
        map.put(MarzConstant.JSON_TAG_RESCODE, resCode);
        map.put(MarzConstant.JSON_TAG_CARDSHOW, cardShow);

        result = null;
        resCode = null;
        return map;
    }
    
    public Map<String, JSONObject> cardFusion(String sid, String baseId, String add_uniqids, String add_cardids) throws Exception
    {
        map = new HashMap<String, JSONObject>();
        String uniqidsStr = ",\"add_uniqids\":[" + add_uniqids + "]";
        String cardidsStr = ",\"add_cardids\":[" + add_cardids + "]";
        
        String paramStr = sid + "={\"base_uniqid\":" + baseId + (CommonUtil.isEmpty(add_uniqids) ? "" : uniqidsStr) + (CommonUtil.isEmpty(add_cardids) ? "" : cardidsStr) + "}";
        
        String[] result = httpRequest.sendPost(MarConstant.URL_CARDFUSION, paramStr).split(MarConstant.KRSMA_SPLIT);
        System.out.println(MarzConstant.LOG_SYSTEM_INFO + "cardFusion2 " + Thread.currentThread().getName());
        
        JSONObject resCode= JSONObject.fromObject(result[1].substring(0, result[1].indexOf("}{") + 1));
        JSONObject cardFusion = JSONObject.fromObject(result[1].substring(result[1].indexOf("}{") + 1, result[1].length()));
        
        map.put(MarzConstant.JSON_TAG_SID, this.sidJSONObject(result[0]));
        map.put(MarzConstant.JSON_TAG_RESCODE, resCode);
        map.put(MarzConstant.JSON_TAG_CARDFUSION, cardFusion);

        result = null;
        resCode = null;
        return map;
    }
    
    public Map<String, JSONObject> cardSell(String sid, String uniqiIds) throws Exception
    {
        map = new HashMap<String, JSONObject>();
        
        String paramStr = sid + "={\"uniqids\":["+uniqiIds+"]}";
        
        String[] result = httpRequest.sendPost(MarConstant.URL_CARDSELL, paramStr).split(MarConstant.KRSMA_SPLIT);
        System.out.println(MarzConstant.LOG_SYSTEM_INFO + "cardSell " + Thread.currentThread().getName());
        
        JSONObject resCode= JSONObject.fromObject(result[1].substring(0, result[1].indexOf("}{") + 1));
        JSONObject cardSell = JSONObject.fromObject(result[1].substring(result[1].indexOf("}{") + 1, result[1].length()));
        
        map.put(MarzConstant.JSON_TAG_SID, this.sidJSONObject(result[0]));
        map.put(MarzConstant.JSON_TAG_RESCODE, resCode);
        map.put(MarzConstant.JSON_TAG_CARDSELL, cardSell);

        result = null;
        resCode = null;
        return map;
    }
    
    public Map<String, JSONObject> presentBoxShow(String sid) throws Exception
    {
        map = new HashMap<String, JSONObject>();
        
        String paramStr = sid + "=";
        String[] result = httpRequest.sendPost(MarConstant.URL_PRESENTBOXSHOW, paramStr).split(MarConstant.KRSMA_SPLIT);
        System.out.println(MarzConstant.LOG_SYSTEM_INFO + "presentBoxShow " + Thread.currentThread().getName());
        
        JSONObject resCode= JSONObject.fromObject(result[1].substring(0, result[1].indexOf("}{") + 1));
        JSONObject presentBoxShow = JSONObject.fromObject(result[1].substring(result[1].indexOf("}{") + 1, result[1].length()));
        
        map.put(MarzConstant.JSON_TAG_SID, this.sidJSONObject(result[0]));
        map.put(MarzConstant.JSON_TAG_RESCODE, resCode);
        map.put(MarzConstant.JSON_TAG_PRESENTBOXSHOW, presentBoxShow);

        result = null;
        resCode = null;
        return map;
    }
    
    public Map<String, JSONObject> presentBoxRecv(String sid, String presentId) throws Exception
    {
        map = new HashMap<String, JSONObject>();
        
        String paramStr = sid + "={\"presentid\":" + presentId + "}";
        String[] result = httpRequest.sendPost(MarConstant.URL_PRESENTBOXRECV, paramStr).split(MarConstant.KRSMA_SPLIT);
        System.out.println(MarzConstant.LOG_SYSTEM_INFO + "presentBoxRecv " + Thread.currentThread().getName());
        
        JSONObject resCode= JSONObject.fromObject(result[1].substring(0, result[1].indexOf("}{") + 1));
        JSONObject presentBoxRecv = JSONObject.fromObject(result[1].substring(result[1].indexOf("}{") + 1, result[1].length()));
        
        map.put(MarzConstant.JSON_TAG_SID, this.sidJSONObject(result[0]));
        map.put(MarzConstant.JSON_TAG_RESCODE, resCode);
        map.put(MarzConstant.JSON_TAG_PRESENTBOXRECV, presentBoxRecv);

        result = null;
        resCode = null;
        return map;
    }
    
    public Map<String, JSONObject> presentBoxMultiRecv(String sid) throws Exception
    {
        map = new HashMap<String, JSONObject>();
        
        String paramStr = sid + "=";
        String[] result = httpRequest.sendPost(MarConstant.URL_PRESENTBOXMULTIRECV, paramStr).split(MarConstant.KRSMA_SPLIT);
        System.out.println(MarzConstant.LOG_SYSTEM_INFO + "presentBoxMultiRecv " + Thread.currentThread().getName());
        
        JSONObject resCode= JSONObject.fromObject(result[1].substring(0, result[1].indexOf("}{") + 1));
        JSONObject presentBoxMultiRecv = JSONObject.fromObject(result[1].substring(result[1].indexOf("}{") + 1, result[1].length()));
        
        map.put(MarzConstant.JSON_TAG_SID, this.sidJSONObject(result[0]));
        map.put(MarzConstant.JSON_TAG_RESCODE, resCode);
        map.put(MarzConstant.JSON_TAG_PRESENTBOXMULTIRECV, presentBoxMultiRecv);

        result = null;
        resCode = null;
        return map;
    }
    
    public Map<String, JSONObject> teamBattleSoloShow(String sid, String arthurType) throws Exception
    {
        map = new HashMap<String, JSONObject>();
        
        String paramStr = sid + "={\"active_arthur_type\":" + arthurType + "}";
        String[] result = httpRequest.sendPost(MarConstant.URL_TEAMBATTLESOLOSHOW, paramStr).split(MarConstant.KRSMA_SPLIT);
        System.out.println(MarzConstant.LOG_SYSTEM_INFO + "teamBattleSoloShow " + Thread.currentThread().getName());
        
        // 有好友信息 必须设置过滤
        result = this.requestFilter(result);
        
        JSONObject resCode= JSONObject.fromObject(result[1].substring(0, result[1].indexOf("}{") + 1));
        JSONObject teamBattleSoloShow = JSONObject.fromObject(result[1].substring(result[1].indexOf("}{") + 1, result[1].length()));
        
        map.put(MarzConstant.JSON_TAG_SID, this.sidJSONObject(result[0]));
        map.put(MarzConstant.JSON_TAG_RESCODE, resCode);
        map.put(MarzConstant.JSON_TAG_TEAMBATTLESOLOSHOW, teamBattleSoloShow);

        result = null;
        resCode = null;
        return map;
    }
    
    public Map<String, JSONObject> teamBattleSoloPartnerShow(String sid, String bossId) throws Exception
    {
        map = new HashMap<String, JSONObject>();
        
        String paramStr = sid + "={\"bossid\":" + bossId + "}";
        
        String[] result = httpRequest.sendPost(MarConstant.URL_TEAMBATTLESOLOPARTNERSHOW, paramStr).split(MarConstant.KRSMA_SPLIT);
        System.out.println(MarzConstant.LOG_SYSTEM_INFO + "teamBattleSoloPartnerShow " + Thread.currentThread().getName());
        
        // 有好友信息 必须设置过滤
        result = this.requestFilter(result);
        
        JSONObject resCode= JSONObject.fromObject(result[1].substring(0, result[1].indexOf("}{") + 1));
        JSONObject teamBattleSoloPartnerShow = JSONObject.fromObject(result[1].substring(result[1].indexOf("}{") + 1, result[1].length()));
        
        map.put(MarzConstant.JSON_TAG_SID, this.sidJSONObject(result[0]));
        map.put(MarzConstant.JSON_TAG_RESCODE, resCode);
        map.put(MarzConstant.JSON_TAG_TEAMBATTLESOLOPARTNERSHOW, teamBattleSoloPartnerShow);

        result = null;
        resCode = null;
        return map;
    }
    
    public Map<String, JSONObject> teamBattleSoloStart(String sid, String bossId, String arthur_type, String userOne, String userTwo, String userThree, String userFour) throws Exception
    {
        map = new HashMap<String, JSONObject>();
        
        List<String> typeList = new ArrayList<String>(Arrays.asList("1", "2", "3", "4"));
        Map<String, String> userIdMap = new HashMap<String, String>();
        userIdMap.put("1", userOne);
        userIdMap.put("2", userTwo);
        userIdMap.put("3", userThree);
        userIdMap.put("4", userFour);
        for (String type : typeList)
        {
            if (type.equals(arthur_type))
            {
                typeList.remove(type);
                break;
            }
        }
        
        String paramStr = sid + "={\"bossid\":" + bossId + ",\"deck_arthur_type\":" + arthur_type + ",\"deck_arthur_type_idx\":0,\"partner_deck_selects\":["
                                          + "{\"userid\":" + userIdMap.get(typeList.get(0)) + ",\"arthur_type\":" + typeList.get(0) + ",\"deck_idx\":0},"
                                          + "{\"userid\":" + userIdMap.get(typeList.get(1)) + ",\"arthur_type\":" + typeList.get(1) + ",\"deck_idx\":0},"
                                          + "{\"userid\":" + userIdMap.get(typeList.get(2)) + ",\"arthur_type\":" + typeList.get(2) + ",\"deck_idx\":0}]}";
        
        String[] result = httpRequest.sendPost(MarConstant.URL_TEAMBATTLESOLOSTART, paramStr).split(MarConstant.KRSMA_SPLIT);
        System.out.println(MarzConstant.LOG_SYSTEM_INFO + "teamBattleSoloStart " + Thread.currentThread().getName());
        
        JSONObject resCode= JSONObject.fromObject(result[1].substring(0, result[1].indexOf("}{") + 1));
        JSONObject teamBattleSoloStart = JSONObject.fromObject(result[1].substring(result[1].indexOf("}{") + 1, result[1].length()));
        
        map.put(MarzConstant.JSON_TAG_SID, this.sidJSONObject(result[0]));
        map.put(MarzConstant.JSON_TAG_RESCODE, resCode);
        map.put(MarzConstant.JSON_TAG_TEAMBATTLESOLOSTART, teamBattleSoloStart);

        result = null;
        resCode = null;
        return map;
    }
    
    public Map<String, JSONObject> teamBattleSoloEnd(String sid, String battleInfo) throws Exception
    {
        map = new HashMap<String, JSONObject>();
        
        String paramStr = sid + "=" + battleInfo;
        
        String[] result = httpRequest.sendPost(MarConstant.URL_TEAMBATTLESOLOEND, paramStr).split(MarConstant.KRSMA_SPLIT);
        System.out.println(MarzConstant.LOG_SYSTEM_INFO + "teamBattleSoloEnd " + Thread.currentThread().getName());
        
        // 有问候语信息 必须设置过滤
        result = this.requestFilter(result);
        
        JSONObject resCode= JSONObject.fromObject(result[1].substring(0, result[1].indexOf("}{") + 1));
        JSONObject teamBattleSoloEnd = JSONObject.fromObject(result[1].substring(result[1].indexOf("}{") + 1, result[1].length()));
        
        map.put(MarzConstant.JSON_TAG_SID, this.sidJSONObject(result[0]));
        map.put(MarzConstant.JSON_TAG_RESCODE, resCode);
        map.put(MarzConstant.JSON_TAG_TEAMBATTLESOLOEND, teamBattleSoloEnd);

        result = null;
        resCode = null;
        return map;
    }
    
    public Map<String, JSONObject> userBuffExec(String sid, String buffId) throws Exception
    {
        map = new HashMap<String, JSONObject>();
        
        String paramStr = sid + "={\"user_buff_id\":" + buffId + "}";
        
        String[] result = httpRequest.sendPost(MarConstant.URL_USERBUFFEXEC, paramStr).split(MarConstant.KRSMA_SPLIT);
        System.out.println(MarzConstant.LOG_SYSTEM_INFO + "userBuffExec " + Thread.currentThread().getName());
        
        JSONObject resCode= JSONObject.fromObject(result[1].substring(0, result[1].indexOf("}{") + 1));
        
        map.put(MarzConstant.JSON_TAG_SID, this.sidJSONObject(result[0]));
        map.put(MarzConstant.JSON_TAG_RESCODE, resCode);

        result = null;
        return map;
    }
    
    public Map<String, JSONObject> pvpShow(String sid) throws Exception
    {
        map = new HashMap<String, JSONObject>();
        
        String paramStr = sid + "=";
        
        String[] result = httpRequest.sendPost(MarConstant.URL_PVPSHOW, paramStr).split(MarConstant.KRSMA_SPLIT);
        System.out.println(MarzConstant.LOG_SYSTEM_INFO + "pvpShow " + Thread.currentThread().getName());
        
        // 有名字 必须设置过滤
        result = this.requestFilter(result);
        
        JSONObject resCode= JSONObject.fromObject(result[1].substring(0, result[1].indexOf("}{") + 1));
        JSONObject pvpShow = JSONObject.fromObject(result[1].substring(result[1].indexOf("}{") + 1, result[1].length()));
        
        map.put(MarzConstant.JSON_TAG_SID, this.sidJSONObject(result[0]));
        map.put(MarzConstant.JSON_TAG_RESCODE, resCode);
        map.put(MarzConstant.JSON_TAG_PVPSHOW, pvpShow);

        result = null;
        resCode = null;
        return map;
    }
    
    /**
     * 
     * @Title: pvpStart
     * @Description PVP战斗开始
     * @param sid
     * @param arthur_type 主选亚瑟类型 1 佣兵；2 富豪；3 盗贼；4 歌姬
     * @param deckMap 卡组ID字符串的MAP 10个卡ID 逗号连接 Map Key为亚瑟类型的1,2,3,4
     * @return
     * @throws Exception
     * @return Map<String,JSONObject> 返回类型
     * @throws
     */
    public Map<String, JSONObject> pvpStart(String sid, String pvpType, String arthur_type, Map<String, DeckEvt> deckMap) throws Exception
    {
        map = new HashMap<String, JSONObject>();
        
        List<String> typeList = new ArrayList<String>(Arrays.asList("1", "2", "3", "4"));
        for (String type : typeList)
        {
            if (type.equals(arthur_type))
            {
                typeList.remove(type);
                break;
            }
        }
        
        String startInfo = "{\"type\":" + pvpType + ",\"pvp_my_deck\":["
            + "{\"arthur_type\":" + arthur_type + ",\"job_type\":" + arthur_type + ",\"deck_idx\":0,\"leader_card_idx\":0,\"card_uniqid\":" + deckMap.get(arthur_type).getCard_uniqid() + ",\"support_card_uniqid\":" + deckMap.get(arthur_type).getSupport_card_uniqid() + ",\"sphr_uniqid\":" + deckMap.get(arthur_type).getSphr_uniqid() + "},"
            + "{\"arthur_type\":" + typeList.get(0) + ",\"job_type\":" + typeList.get(0) + ",\"deck_idx\":0,\"leader_card_idx\":0,\"card_uniqid\":" + deckMap.get(typeList.get(0)).getCard_uniqid() + ",\"support_card_uniqid\":" + deckMap.get(typeList.get(0)).getSupport_card_uniqid() + ",\"sphr_uniqid\":" + deckMap.get(typeList.get(0)).getSphr_uniqid() + "},"
            + "{\"arthur_type\":" + typeList.get(1) + ",\"job_type\":" + typeList.get(1) + ",\"deck_idx\":0,\"leader_card_idx\":0,\"card_uniqid\":" + deckMap.get(typeList.get(1)).getCard_uniqid() + ",\"support_card_uniqid\":" + deckMap.get(typeList.get(1)).getSupport_card_uniqid() + ",\"sphr_uniqid\":" + deckMap.get(typeList.get(1)).getSphr_uniqid() + "},"
            + "{\"arthur_type\":" + typeList.get(2) + ",\"job_type\":" + typeList.get(2) + ",\"deck_idx\":0,\"leader_card_idx\":0,\"card_uniqid\":" + deckMap.get(typeList.get(2)).getCard_uniqid() + ",\"support_card_uniqid\":" + deckMap.get(typeList.get(2)).getSupport_card_uniqid() + ",\"sphr_uniqid\":" + deckMap.get(typeList.get(2)).getSphr_uniqid() + "}]}";
        
        String paramStr = sid + "=" + startInfo;
        
        String[] result = httpRequest.sendPost(MarConstant.URL_PVPSTART, paramStr).split(MarConstant.KRSMA_SPLIT);
        System.out.println(MarzConstant.LOG_SYSTEM_INFO + "pvpStart " + Thread.currentThread().getName());
        
        // 有名字 必须设置过滤
        result = this.requestFilter(result);
        
        JSONObject resCode= JSONObject.fromObject(result[1].substring(0, result[1].indexOf("}{") + 1));
        JSONObject pvpStart = JSONObject.fromObject(result[1].substring(result[1].indexOf("}{") + 1, result[1].length()));
        
        map.put(MarzConstant.JSON_TAG_SID, this.sidJSONObject(result[0]));
        map.put(MarzConstant.JSON_TAG_RESCODE, resCode);
        map.put(MarzConstant.JSON_TAG_PVPSTART, pvpStart);

        result = null;
        resCode = null;
        return map;
    }
    
    public Map<String, JSONObject> pvpEnd(String sid, String btluid, String pvpEndStr) throws Exception
    {
        map = new HashMap<String, JSONObject>();
        
        String endInfo = "{\"btluid\":" + btluid + pvpEndStr;
        
        String paramStr = sid + "=" + endInfo;
        
        String[] result = httpRequest.sendPost(MarConstant.URL_PVPEND, paramStr).split(MarConstant.KRSMA_SPLIT);
        System.out.println(MarzConstant.LOG_SYSTEM_INFO + "pvpEnd " + Thread.currentThread().getName());
        
        JSONObject resCode= JSONObject.fromObject(result[1].substring(0, result[1].indexOf("}{") + 1));
        JSONObject pvpEnd = JSONObject.fromObject(result[1].substring(result[1].indexOf("}{") + 1, result[1].length()));
        
        map.put(MarzConstant.JSON_TAG_SID, this.sidJSONObject(result[0]));
        map.put(MarzConstant.JSON_TAG_RESCODE, resCode);
        map.put(MarzConstant.JSON_TAG_PVPEND, pvpEnd);

        result = null;
        resCode = null;
        return map;
    }
    
    /**
     * 
     * @Title: pvpStart
     * @Description PVP战斗开始
     * @param sid
     * @param arthur_type 主选亚瑟类型 1 佣兵；2 富豪；3 盗贼；4 歌姬
     * @param deckMap 卡组ID字符串的MAP 10个卡ID 逗号连接 Map Key为亚瑟类型的1,2,3,4
     * @return
     * @throws Exception
     * @return Map<String,JSONObject> 返回类型
     * @throws
     */
    public Map<String, JSONObject> pvpStart2(String sid, String pvpType, String arthur_type, Map<String, DeckEvt> deckMap) throws Exception
    {
        map = new HashMap<String, JSONObject>();
        
        List<String> typeList = new ArrayList<String>(Arrays.asList("1", "2", "3", "4"));
        for (String type : typeList)
        {
            if (type.equals(arthur_type))
            {
                typeList.remove(type);
                break;
            }
        }
        
        String startInfo = "{\"type\":" + pvpType + ",\"select_arthur_type\":" + arthur_type + ",\"pvp_my_deck\":["
            + "{\"arthur_type\":" + arthur_type + ",\"job_type\":" + arthur_type + ",\"deck_idx\":0,\"leader_card_idx\":0,\"card_uniqid\":" + deckMap.get(arthur_type).getCard_uniqid() + ",\"support_card_uniqid\":" + deckMap.get(arthur_type).getSupport_card_uniqid() + ",\"sphr_uniqid\":" + deckMap.get(arthur_type).getSphr_uniqid() + "},"
            + "{\"arthur_type\":" + typeList.get(0) + ",\"job_type\":" + typeList.get(0) + ",\"deck_idx\":0,\"leader_card_idx\":0,\"card_uniqid\":" + deckMap.get(typeList.get(0)).getCard_uniqid() + ",\"support_card_uniqid\":" + deckMap.get(typeList.get(0)).getSupport_card_uniqid() + ",\"sphr_uniqid\":" + deckMap.get(typeList.get(0)).getSphr_uniqid() + "},"
            + "{\"arthur_type\":" + typeList.get(1) + ",\"job_type\":" + typeList.get(1) + ",\"deck_idx\":0,\"leader_card_idx\":0,\"card_uniqid\":" + deckMap.get(typeList.get(1)).getCard_uniqid() + ",\"support_card_uniqid\":" + deckMap.get(typeList.get(1)).getSupport_card_uniqid() + ",\"sphr_uniqid\":" + deckMap.get(typeList.get(1)).getSphr_uniqid() + "},"
            + "{\"arthur_type\":" + typeList.get(2) + ",\"job_type\":" + typeList.get(2) + ",\"deck_idx\":0,\"leader_card_idx\":0,\"card_uniqid\":" + deckMap.get(typeList.get(2)).getCard_uniqid() + ",\"support_card_uniqid\":" + deckMap.get(typeList.get(2)).getSupport_card_uniqid() + ",\"sphr_uniqid\":" + deckMap.get(typeList.get(2)).getSphr_uniqid() + "}]}";
        
        String paramStr = sid + "=" + startInfo;
        
        String[] result = httpRequest.sendPost(MarConstant.URL_PVPSTART2, paramStr).split(MarConstant.KRSMA_SPLIT);
        System.out.println(MarzConstant.LOG_SYSTEM_INFO + "pvpStart2 " + Thread.currentThread().getName());
        
        // 有名字 必须设置过滤
        result = this.requestFilter(result);
        
        JSONObject resCode= JSONObject.fromObject(result[1].substring(0, result[1].indexOf("}{") + 1));
        JSONObject pvpStart = JSONObject.fromObject(result[1].substring(result[1].indexOf("}{") + 1, result[1].length()));
        
        map.put(MarzConstant.JSON_TAG_SID, this.sidJSONObject(result[0]));
        map.put(MarzConstant.JSON_TAG_RESCODE, resCode);
        map.put(MarzConstant.JSON_TAG_PVPSTART, pvpStart);

        result = null;
        resCode = null;
        return map;
    }
    
    /**
     * 
     * @Title: missionShow
     * @Description: state 0 未完成；1 完成为领取；2 完成已领取
     * @param sid
     * @return
     * @throws Exception
     * @return Map<String,JSONObject> 返回类型
     * @throws
     */
    public Map<String, JSONObject> missionShow(String sid) throws Exception
    {
        map = new HashMap<String, JSONObject>();
        
        String paramStr = sid + "=" + "{\"is_reward\":1}";
        String[] result = httpRequest.sendPost(MarConstant.URL_MISSIONSHOW, paramStr).split(MarConstant.KRSMA_SPLIT);
        System.out.println(MarzConstant.LOG_SYSTEM_INFO + "missionShow " + Thread.currentThread().getName());
        
        // 有任务信息 必须设置过滤
        result = this.requestFilter(result);
        
        JSONObject resCode= JSONObject.fromObject(result[1].substring(0, result[1].indexOf("}{") + 1));
        JSONObject missionShow = JSONObject.fromObject(result[1].substring(result[1].indexOf("}{") + 1, result[1].length()));
        
        map.put(MarzConstant.JSON_TAG_SID, this.sidJSONObject(result[0]));
        map.put(MarzConstant.JSON_TAG_RESCODE, resCode);
        map.put(MarzConstant.JSON_TAG_MISSIONSHOW, missionShow);

        result = null;
        resCode = null;
        return map;
    }
    
    public Map<String, JSONObject> missionReward(String sid, String missionids) throws Exception
    {
        map = new HashMap<String, JSONObject>();
        
        String paramStr = sid + "=" + "{\"missionids\":[" + missionids + "]}";
        String[] result = httpRequest.sendPost(MarConstant.URL_MISSIONREWARD, paramStr).split(MarConstant.KRSMA_SPLIT);
        System.out.println(MarzConstant.LOG_SYSTEM_INFO + "missionReward " + Thread.currentThread().getName());
        
        // 有任务信息 必须设置过滤
        result = this.requestFilter(result);
        
        JSONObject resCode= JSONObject.fromObject(result[1].substring(0, result[1].indexOf("}{") + 1));
        JSONObject missionReward = JSONObject.fromObject(result[1].substring(result[1].indexOf("}{") + 1, result[1].length()));
        
        map.put(MarzConstant.JSON_TAG_SID, this.sidJSONObject(result[0]));
        map.put(MarzConstant.JSON_TAG_RESCODE, resCode);
        map.put(MarzConstant.JSON_TAG_MISSIONREWARD, missionReward);

        result = null;
        resCode = null;
        return map;
    }
    
    public Map<String, JSONObject> gachaPlay(String sid, String gachaId, String payType, String gachaHash) throws Exception
    {
        map = new HashMap<String, JSONObject>();
        
        String paramStr = sid + "=" + "{\"gachaid\":" + gachaId + ",\"pay_type\":" + payType + ",\"gacha_hash\":\"" + gachaHash + "\"}";
        String[] result = httpRequest.sendPost(MarConstant.URL_GACHAPLAY, paramStr).split(MarConstant.KRSMA_SPLIT);
        System.out.println(MarzConstant.LOG_SYSTEM_INFO + "gachaPlay " + Thread.currentThread().getName());
        
        JSONObject resCode= JSONObject.fromObject(result[1].substring(0, result[1].indexOf("}{") + 1));
        JSONObject gachaPlay = JSONObject.fromObject(result[1].substring(result[1].indexOf("}{") + 1, result[1].length()));
        
        map.put(MarzConstant.JSON_TAG_SID, this.sidJSONObject(result[0]));
        map.put(MarzConstant.JSON_TAG_RESCODE, resCode);
        map.put(MarzConstant.JSON_TAG_GACHAPLAY, gachaPlay);

        result = null;
        resCode = null;
        return map;
    }
    
    private String[] requestFilter(String[] result)
    {
        if (result.length > 2)
        {
            for (int i = 2; i < result.length; i++)
            {
                result[1] += result[i];
            }
        }
        
        return result;
    }
    
    public static void main(String[] args) throws Exception
    {
        /*
        String str = "{xxx}{xxxxxxx}";
        System.out.println(str.substring(0, str.indexOf("}{") + 1));
        System.out.println(str.substring(str.indexOf("}{") + 1, str.length()));
        String uuid = "f57e8b2c-acbf-43a7-9d05-a3f0fc439a46";
        String token = "492N+ZrLTxcvj3h/gWTjdX/+RJE=";
        
        List<String> typeList = new ArrayList<String>(Arrays.asList("1", "2", "3", "4"));
        for (String type : typeList)
        {
            if (type.equals("3"))
            {
                typeList.remove(type);
                break;
            }
        }
        System.out.println(typeList.get(0)+typeList.get(1)+typeList.get(2));
        */
    	
    	MarzRequest request = new MarzRequest();
    	String androidUuid = "7027063d-7186-4dd0-a496-32b5c991acbd";
        String androidToken = "JKft/CxeVmzYMbYlmF2aY/Q6iLc=";
        System.out.println(request.loginAndroid(androidUuid, androidToken));
        /*
    	MarzRequest request = new MarzRequest();
        String iosUuid = "920507d8-4987-4400-bcd0-6922091cee11";
        String iosToken = "4ut9NO4FkxiXqOKRUtHXk7/bTYs=";
        System.out.println(request.loginIOS(iosUuid, iosToken));
        */
    }
}
