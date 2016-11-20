package org.cleverframe.fastdfs.pool;

import org.cleverframe.fastdfs.conn.Connection;
import org.cleverframe.fastdfs.exception.FastDFSException;
import org.cleverframe.fastdfs.protocol.FastDFSCommand;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.net.InetSocketAddress;

/**
 * 连接池管理<br/>
 * 负责借出连接，在连接上执行业务逻辑，然后归还连<br/>
 * 作者：LiZW <br/>
 * 创建时间：2016/11/20 19:26 <br/>
 */
public class ConnectionManager {
    /**
     * 日志
     */
    private static final Logger logger = LoggerFactory.getLogger(ConnectionManager.class);

    /**
     * 连接池
     */
    private FastDFSConnectionPool pool;

    /**
     * 构造函数
     */
    ConnectionManager() {
        super();
    }

    /**
     * 构造函数
     *
     * @param pool 连接池
     */
    public ConnectionManager(FastDFSConnectionPool pool) {
        super();
        this.pool = pool;
    }

    /**
     * 获取连接
     *
     * @param address 请求地址
     * @return 连接
     */
    Connection getConnection(InetSocketAddress address) {
        Connection conn;
        try {
            // 获取连接
            conn = pool.borrowObject(address);
        } catch (FastDFSException e) {
            throw e;
        } catch (Exception e) {
            throw new RuntimeException("从连接池中获取连接异常", e);
        }
        return conn;
    }

    /**
     * 获取连接并执行交易
     *
     * @param address 请求地址
     * @param command 请求命令
     * @return 返回请求结果数据
     */
    public <T> T executeCmd(InetSocketAddress address, FastDFSCommand<T> command) {
        // 获取连接
        Connection conn = getConnection(address);
        // 执行交易
        return execute(address, conn, command);
    }

    /**
     * 执行交易
     *
     * @param address 请求地址
     * @param conn    请求连接
     * @param command 请求命令
     * @return 返回请求结果数据
     */
    protected <T> T execute(InetSocketAddress address, Connection conn, FastDFSCommand<T> command) {
        try {
            // 发送请求
            logger.debug("对地址[{}] 发送请求[{}]", address, command.getClass().getSimpleName());
            return command.execute(conn);
        } catch (FastDFSException e) {
            throw e;
        } catch (Exception e) {
            throw new RuntimeException("发送FastDFS请求异常", e);
        } finally {
            try {
                if (null != conn) {
                    pool.returnObject(address, conn);
                }
            } catch (Exception e) {
                logger.error("归还连接到连接池失败", e);
            }
        }
    }

    /**
     * 获取连接池信息
     *
     * @param address 请求地址
     */
    public void dumpPoolInfo(InetSocketAddress address) {
        if (logger.isDebugEnabled()) {
            String tmp = "\r\n" +
                    "#=======================================================================================================================#\r\n" +
                    "# ------Dump Pool Info------\r\n" +
                    "#\t 活动连接：" + pool.getNumActive(address) + "\r\n" +
                    "#\t 空闲连接：" + pool.getNumIdle(address) + "\r\n" +
                    "#\t 连接获取总数统计：" + pool.getBorrowedCount() + "\r\n" +
                    "#\t 连接返回总数统计：" + pool.getReturnedCount() + "\r\n" +
                    "#\t 连接销毁总数统计：" + pool.getDestroyedCount() + "\r\n" +
                    "#=======================================================================================================================#\r\n";
            logger.debug(tmp);
        }
    }

    public FastDFSConnectionPool getPool() {
        return pool;
    }

    public void setPool(FastDFSConnectionPool pool) {
        this.pool = pool;
    }
}