import org.apache.ibatis.executor.*;
import org.apache.ibatis.mapping.BoundSql;
import org.apache.ibatis.mapping.MappedStatement;
import org.apache.ibatis.session.*;
import org.apache.ibatis.transaction.Transaction;
import org.apache.ibatis.transaction.managed.ManagedTransaction;
import org.junit.Before;
import org.junit.Test;

import java.io.InputStream;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.util.List;
import java.util.Map;

/**
 * @Description:
 * @Author: xianchao.hua
 * @Create: 2021-01-07 08:40
 */
public class DebugMainTest {

    private Configuration configuration;
    private SqlSessionFactory sqlSessionFactory;
    private Connection connection;
    private MappedStatement getByIdStatement;
    private MappedStatement updateStatement;

    @Before
    public void init() throws SQLException {
        InputStream inputStream = DebugMainTest.class.getResourceAsStream("mybatis-config.xml");
        sqlSessionFactory = new SqlSessionFactoryBuilder().build(inputStream);
        configuration = sqlSessionFactory.getConfiguration();
        getByIdStatement = configuration.getMappedStatement("com.hy.mapper.UserMapper.getById");
        updateStatement = configuration.getMappedStatement("com.hy.mapper.UserMapper.update");
        connection = DriverManager.getConnection("jdbc:mysql://localhost:3306/test_study?serverTimezone=GMT%2b8", "root", "123456");
    }

    @Test
    public void testSimpleExector() throws SQLException {
        SimpleExecutor executor = new SimpleExecutor(configuration, new ManagedTransaction(connection, true));
        //执行两次查询--直接调用SimpleExector的doQuery方法，所以不涉及缓存，相同的Sql被预编译两次，执行两次
        List<Object> result1 = executor.doQuery(getByIdStatement, 2, RowBounds.DEFAULT, null, null);
        List<Object> result2 = executor.doQuery(getByIdStatement, 2, RowBounds.DEFAULT, null, null);
        System.out.println(result1);
        System.out.println(result2);
    }

    @Test
    public void testReuseExector() throws SQLException {
        ReuseExecutor executor = new ReuseExecutor(configuration, new ManagedTransaction(connection, true));
        //执行两次查询--直接调用ReuseExector的doQuery方法，所以不涉及缓存，相同的Sql被预编译一次，执行两次
        List<Object> result1 = executor.doQuery(getByIdStatement, 2, RowBounds.DEFAULT, null, null);
        List<Object> result2 = executor.doQuery(getByIdStatement, 2, RowBounds.DEFAULT, null, null);
        System.out.println(result1);
        System.out.println(result2);
    }

    @Test
    public void testBatchExector1() throws SQLException {
        BatchExecutor executor = new BatchExecutor(configuration, new ManagedTransaction(connection, true));
        //执行两次查询--直接调用BatchExector的doQuery方法，所以不涉及缓存，相同的Sql被预编译两次，执行两次，未起到批处理的效果
        //说明-批处理只对update操作有效，对查询语句无效
        List<Object> result1 = executor.doQuery(getByIdStatement, 2, RowBounds.DEFAULT, null, null);
        List<Object> result2 = executor.doQuery(getByIdStatement, 2, RowBounds.DEFAULT, null, null);
        System.out.println(result1);
        System.out.println(result2);
    }

    @Test
    public void testBatchExector2() throws SQLException {
        BatchExecutor executor = new BatchExecutor(configuration, new ManagedTransaction(connection, true));
        //执行两次查询--直接调用BatchExector的doQuery方法，所以不涉及缓存，相同的Sql被预编译一次
        com.hy.VO.User user = new com.hy.VO.User();
        user.setId(2);
        user.setName("name2");
        executor.doUpdate(updateStatement, user);
        executor.doUpdate(updateStatement, user);
        //但是此时 数据库查看更新结果发现数据并没有被更新成功
        //说明，batch执行更新操作需要显式提交
        executor.commit(true);
    }

    //二级缓存
    @Test
    public void testCachExector() throws SQLException {
        SimpleExecutor simpleExecutor = new SimpleExecutor(configuration, new ManagedTransaction(connection, true));
        CachingExecutor cachingExecutor = new CachingExecutor(simpleExecutor);
        cachingExecutor.query(getByIdStatement, 2, RowBounds.DEFAULT, null);
        cachingExecutor.commit(true);
        cachingExecutor.query(getByIdStatement, 2, RowBounds.DEFAULT, null);
        cachingExecutor.query(getByIdStatement, 2, RowBounds.DEFAULT, null);
        cachingExecutor.query(getByIdStatement, 2, RowBounds.DEFAULT, null);
    }
}
