package com.moemao.tgks.mar.marz.thread;

import java.net.InetAddress;
import java.net.UnknownHostException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import org.springframework.beans.BeansException;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;

import com.moemao.tgks.common.core.spring.ContextUtil;
import com.moemao.tgks.mar.marz.task.MarzTaskDiffusion;
import com.moemao.tgks.mar.marz.tool.MarzConstant;
import com.moemao.tgks.mar.marzaccount.entity.MarzAccountEvt;
import com.moemao.tgks.mar.marzaccount.service.MarzAccountService;
import com.moemao.tgks.mar.marzserver.entity.MarzServerEvt;
import com.moemao.tgks.mar.marzserver.service.MarzServerService;
import com.moemao.tgks.mar.tool.MarConstant;

/**
 * 
 * 项目名称：TGKS_MAR
 * 类名称：MarzThreadPoolDiffusion
 * 类描述：与之前的挂机池模式不同，Diffusion采用启动时扫描所有未过期的在线账号并创建对应线程执行，直到池关闭或者用户自行修改账户状态
 * 创建人：Administrator
 * 创建时间：2015-1-27 下午10:08:35
 * 修改人：Administrator
 * 修改时间：2015-1-27 下午10:08:35
 * 修改备注：
 * @version
 *
 */
public class MarzThreadPoolDiffusion implements Runnable, ApplicationContextAware
{
    private MarzThreadPoolDiffusion()
    {
        marzAccountService = (MarzAccountService) ContextUtil.getBean("mar_marzAccountService");
        marzServerService = (MarzServerService) ContextUtil.getBean("mar_marzServerService");
    }
    
    private static MarzThreadPoolDiffusion instance;
    
    public static MarzThreadPoolDiffusion getInstance()
    {
        if (null == instance)
        {
            instance = new MarzThreadPoolDiffusion();
        }
        
        return instance;
    }
    
    private ExecutorService executor = null;
    
    private MarzAccountService marzAccountService;
    
    private MarzServerService marzServerService;
    
    private boolean bRunning = true;
    
    private static final int defaultThreadNum = 50;
    
    private static final int mainThreadSleep = 300000;
    
    private static final int singleTime = 4000;
    
    private List<MarzAccountEvt> accountList;
    
    private ThreadGroup threadGroup;
    
    private Thread[] threads;
    
    public void run()
    {
        bRunning = true;
        
        if (null == executor || executor.isTerminated())
        {
            executor = Executors.newFixedThreadPool(defaultThreadNum);
            //executor = Executors.newScheduledThreadPool(defaultThreadNum);
            System.out.println("线程池创建完毕");
        }
        else
        {
            // 存在当前线程池
        }
        
        try
        {
            // 获取本机IP
            String ip = InetAddress.getLocalHost().getHostAddress().toString();
            // 通过本机IP获取外网IP
            MarzServerEvt marzServerEvt = marzServerService.queryMarzServerByLoaclIp(ip);
            
            // 修改服务器表状态
            marzServerService.changeMarzServerStatus(ip, MarzConstant.MARZSERVER_STATUS_1);
            
            // 从数据库中查询出需要执行的任务 账号信息表中存放的是外网IP
            accountList = marzAccountService.queryMarzAccountOnline(marzServerEvt.getNetIp());
            
            System.out.println("取出需要执行的任务数：" + accountList.size());
            
            // 循环建立新的任务 放入线程池执行
            for (MarzAccountEvt account : accountList)
            {
                // update by ken 20150821 for 负载均衡加上后 启动服务器时只拉起本机上运行的线程
                if (ip.equals(account.getIpAddress()))
                {
                    account.setSessionId("");
                    marzAccountService.updateMarzAccount(account);
                    executor.execute(new MarzTaskDiffusion(account));
                    
                    Thread.sleep(singleTime);
                }
            }
            
            while (bRunning)
            {
                System.out.println("主线程目前正常运行中！" + new SimpleDateFormat("yyyy-MM-dd HH:mm:ss").format(new Date()));
                
                // 定时销毁已经释放了的线程
                this.destoryOverThread();
                
                marzServerService.updateMarzServerUserNum(ip, this.getMarzThreadNum());
                
                Thread.sleep(mainThreadSleep);
            }
        }
        catch (InterruptedException e)
        {
            System.out.println("出问题了！我不行了！" + new SimpleDateFormat("yyyy-MM-dd HH:mm:ss").format(new Date()));
        }
        catch (UnknownHostException e)
        {
            e.printStackTrace();
            System.out.println("服务器IP获取异常！" + new SimpleDateFormat("yyyy-MM-dd HH:mm:ss").format(new Date()));
        }
    }
    
    public boolean createThread(MarzAccountEvt marzAccountEvt)
    {
        if (null == executor || executor.isTerminated())
        {
            // 线程池未启动 返回失败
            return false;
        }
        else
        {
            if (!existThread(MarConstant.MODULE_TAG + marzAccountEvt.getTgksId()))
            {
                // 创建新线程
                executor.execute(new MarzTaskDiffusion(marzAccountEvt));
            }
            else
            {
                System.out.println("已经存在同名的线程！" + marzAccountEvt.getTgksId());
                return false;
            }
        }
        
        return true;
    }
    
    public boolean createThread_new(MarzAccountEvt marzAccountEvt)
    {
        if (!existThread(MarConstant.MODULE_TAG + marzAccountEvt.getTgksId()))
        {
            // 创建新线程
            Thread thread = new Thread(new MarzTaskDiffusion(marzAccountEvt));
            thread.start();
        }
        else
        {
            System.out.println("已经存在同名的线程！" + marzAccountEvt.getTgksId());
            return false;
        }
        
        return true;
    }
    
    public boolean stopThread(String threadName)
    {
        threadGroup = Thread.currentThread().getThreadGroup();
        threads = new Thread[threadGroup.activeCount()];
        threadGroup.enumerate(threads);
        
        for (Thread thread : threads)
        {
            if (thread != null && thread.getName().equals(threadName))
            {
                if (Thread.State.RUNNABLE != thread.getState())
                {
                    try
                    {
                        System.out.println(thread.getName() + "线程关闭中...");
                        thread.setName(thread.getName() + MarzConstant.OVER);
                    }
                    catch (Throwable t)
                    {
                        
                    }
                }
            }
        }
        return false;
    }
    
    @SuppressWarnings("deprecation")
    public void destoryOverThread()
    {
        threadGroup = Thread.currentThread().getThreadGroup();
        threads = new Thread[threadGroup.activeCount()];
        threadGroup.enumerate(threads);
        
        for (Thread thread : threads)
        {
            if (thread != null && thread.getName().equals(MarConstant.MODULE_TAG + MarzConstant.OVER))
            {
                try
                {
                    thread.stop();
                }
                catch (Throwable t)
                {
                    
                }
            }
        }
    }
    
    public boolean existThread(String threadName)
    {
        threadGroup = Thread.currentThread().getThreadGroup();
        threads = new Thread[threadGroup.activeCount()];
        threadGroup.enumerate(threads);
        
        for (Thread thread : threads)
        {
            if (thread != null && thread.getName().contains(threadName))
            {
                try
                {
                    return true;
                }
                catch (Throwable t)
                {
                    
                }
            }
        }
        return false;
    }
    
    @SuppressWarnings("deprecation")
    public void shutdown()
    {
        // 更新服务器状态
        try
        {
            // 获取本机IP
            String ip = InetAddress.getLocalHost().getHostAddress().toString();
            
            // 修改服务器表状态
            marzServerService.changeMarzServerStatus(ip, MarzConstant.MARZSERVER_STATUS_0);
        }
        catch (UnknownHostException e)
        {
            e.printStackTrace();
        }
        
        if (null == executor || executor.isTerminated())
        {
            
        }
        else
        {
            // 存在当前线程池
            // 关闭线程
            executor.shutdown();
            
            threadGroup = Thread.currentThread().getThreadGroup();
            threads = new Thread[threadGroup.activeCount()];
            threadGroup.enumerate(threads);
            
            for (Thread thread : threads)
            {
                if (thread != null && thread.getName().contains(MarConstant.MODULE_TAG))
                {
                    try
                    {
                        thread.stop();
                        System.out.println("关闭线程..." + thread.getName());
                    }
                    catch (Throwable t)
                    {
                        
                    }
                }
            }
        }
        
        System.out.println("线程池已经关闭...");
    }
    
    public List<String> showAllThread()
    {
        List<String> list = new ArrayList<String>();
        ThreadGroup threadGroup = Thread.currentThread().getThreadGroup();
        Thread[] threads = new Thread[threadGroup.activeCount()];
        threadGroup.enumerate(threads);
        
        for (Thread thread : threads)
        {
            System.out.println(thread.getName() + " " + thread.getState());
            if (thread != null && thread.getName().contains(MarConstant.MODULE_TAG))
            {
                list.add(thread.getName() + " " + thread.getState());
            }
        }
        
        return list;
    }
    
    public int getMarzThreadNum()
    {
        int num = 0;
        ThreadGroup threadGroup = Thread.currentThread().getThreadGroup();
        Thread[] threads = new Thread[threadGroup.activeCount()];
        threadGroup.enumerate(threads);
        
        for (Thread thread : threads)
        {
            if (thread != null && thread.getName().contains(MarConstant.MODULE_TAG))
            {
                num++;
            }
        }
        
        return num;
    }
    
    public boolean isbRunning()
    {
        return bRunning;
    }

    public void setbRunning(boolean bRunning)
    {
        this.bRunning = bRunning;
    }
    
    public MarzAccountService getMarzAccountService()
    {
        return marzAccountService;
    }

    public void setMarzAccountService(MarzAccountService marzAccountService)
    {
        this.marzAccountService = marzAccountService;
    }

    @Override
    public void setApplicationContext(ApplicationContext applicationContext)
            throws BeansException
    {
        ContextUtil.setApplicationContext(applicationContext);
    }

    public static void main(String[] args)
    {
        InetAddress addr;
        try
        {
            addr = InetAddress.getLocalHost();
            String ip=addr.getHostAddress().toString();
            System.out.println(ip);
            
            InetAddress[]   inetAdds   =   InetAddress.getAllByName(InetAddress.getLocalHost().getHostName()); 
            for(int i = 0 ; i < inetAdds.length; i++){
                System.out.print(inetAdds[i].getHostName()+ ":\t");
                System.out.println(inetAdds[i].getHostAddress());
            }
        }
        catch (UnknownHostException e)
        {
        }
    }
}
