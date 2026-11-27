package com.papergen.generate.domain.model;

import io.swagger.v3.oas.annotations.media.Schema;

import java.io.Serializable;

/**
 * 表格分页数据对象
 * 
 * @author ruoyi
 */
@Schema(description = "表格分页数据对象")
public class TableDataInfo<T> implements Serializable
{
    private static final long serialVersionUID = 1L;

    /** 总记录数 */
    @Schema(description = "总记录数")
    private long total;

    /** 列表数据 */
    @Schema(description = "列表数据")
    private T rows;

    /** 消息状态码 */
    @Schema(description = "消息状态码")
    private int code;

    /** 消息内容 */
    @Schema(description = "消息内容")
    private String msg;

    /**
     * 表格数据对象
     */
    public TableDataInfo()
    {
    }

    /**
     * 分页
     * 
     * @param rows 列表数据
     * @param total 总记录数
     */
    public TableDataInfo(T rows, long total)
    {
        this.rows = rows;
        this.total = total;
    }

    public long getTotal()
    {
        return total;
    }

    public void setTotal(long total)
    {
        this.total = total;
    }

    public T getRows()
    {
        return rows;
    }

    public void setRows(T rows)
    {
        this.rows = rows;
    }

    public int getCode()
    {
        return code;
    }

    public void setCode(int code)
    {
        this.code = code;
    }

    public String getMsg()
    {
        return msg;
    }

    public void setMsg(String msg)
    {
        this.msg = msg;
    }
}
