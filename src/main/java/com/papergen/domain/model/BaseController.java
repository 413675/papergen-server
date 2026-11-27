package com.papergen.domain.model;


import com.papergen.constant.HttpStatus;
import lombok.extern.slf4j.Slf4j;


/**
 * web层通用数据处理
 * 
 * @author ruoyi
 */
@Slf4j
public class BaseController
{

    /**
     * 响应请求分页数据
     */
    @SuppressWarnings({ "rawtypes", "unchecked" })
    protected <T>TableDataInfo getDataTable(T list,Long total)
    {
        TableDataInfo rspData = new TableDataInfo<T>();
        rspData.setCode(HttpStatus.SUCCESS);
        rspData.setMsg("查询成功");
        rspData.setRows(list);
        rspData.setTotal(total);
        return rspData;
    }


    /**
     * 响应返回结果
     *
     * @param result 结果
     * @return 操作结果
     */
    protected R toResult(boolean result)
    {
        return result ? R.ok() : R.fail();
    }

    /**
     * 响应返回结果
     *
     * @param result 结果
     * @return 操作结果
     */
    protected R toResult(int result)
    {
        return result > 0 ? R.ok() : R.fail();
    }


}
