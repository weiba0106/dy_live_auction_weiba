package com.dy.liveauction.service.auction;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.dy.liveauction.dao.entity.AuctionItem;
import com.dy.liveauction.service.auction.dto.AuctionCreateRequest;
import com.dy.liveauction.service.auction.dto.AuctionDetailVO;

import java.math.BigDecimal;
import java.util.List;

public interface AuctionService {

    /** 发布竞拍商品 */
    AuctionItem createAuction(Long merchantId, AuctionCreateRequest req);

    /** 修改未开始的竞拍规则 */
    void updateAuction(Long itemId, AuctionCreateRequest req);

    /** 查看竞拍详情 */
    AuctionDetailVO getDetail(Long itemId);

    /** 某直播间下竞拍中的商品列表 */
    List<AuctionDetailVO> listByRoom(Long roomId);

    /** 分页查我发布的竞拍 */
    IPage<AuctionDetailVO> pageMyAuctions(Long merchantId, int page, int size);

    /** 开始竞拍 */
    void startAuction(Long itemId);

    /** 用户出价 */
    AuctionItem placeBid(Long itemId, Long userId, BigDecimal amount);

    /** 结束竞拍 */
    void endAuction(Long itemId);

    /** 取消竞拍 */
    void cancelAuction(Long itemId, String reason);
}
