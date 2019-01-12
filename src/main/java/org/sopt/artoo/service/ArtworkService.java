package org.sopt.artoo.service;

import lombok.extern.slf4j.Slf4j;
import org.sopt.artoo.dto.*;
import org.sopt.artoo.mapper.*;
import org.sopt.artoo.model.*;
import org.sopt.artoo.mapper.ArtworkMapper;
import org.sopt.artoo.mapper.ArtworkPicMapper;
import org.sopt.artoo.utils.ResponseMessage;
import org.sopt.artoo.utils.StatusCode;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.interceptor.TransactionAspectSupport;
import org.springframework.util.ObjectUtils;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.List;

@Slf4j
@Service
public class ArtworkService {

    private final ArtworkMapper artworkMapper;
    private final ArtworkPicMapper artworkPicMapper;
    private final UserMapper userMapper;
    private final ArtworkLikeMapper artworkLikeMapper;
    private final S3FileUploadService s3FileUploadService;
    private final PurchaseMapper purchaseMapper;
    private final CommentMapper commentMapper;
    private final DisplayContentMapper displayContentMapper;
    private final DisplayMapper displayMapper;

    public ArtworkService(ArtworkMapper artworkMapper, ArtworkPicMapper artworkPicMapper, UserMapper userMapper, ArtworkLikeMapper artworkLikeMapper, S3FileUploadService s3FileUploadService, PurchaseMapper purchaseMapper, CommentMapper commentMapper, DisplayContentMapper displayContentMapper, DisplayMapper displayMapper) {
        this.artworkMapper = artworkMapper;
        this.artworkPicMapper = artworkPicMapper;
        this.userMapper = userMapper;
        this.artworkLikeMapper = artworkLikeMapper;
        this.s3FileUploadService = s3FileUploadService;
        this.purchaseMapper = purchaseMapper;
        this.commentMapper = commentMapper;
        this.displayContentMapper = displayContentMapper;
        this.displayMapper = displayMapper;
    }

    /**
     * 모든 작품 조회
     *
     * @return DefaultRes
     */
    public DefaultRes<List<Artwork>> findAll(final int a_idx) {
        List<Artwork> artworkList = artworkMapper.findAll(a_idx);
        final int numArtwork = artworkMapper.findRealAll().size();
        for (Artwork artwork : artworkList) {
            artwork.setPic_url(artworkPicMapper.findByArtIdx(artwork.getA_idx()).getPic_url());
        }
        return DefaultRes.res(StatusCode.OK, ResponseMessage.READ_ALL_CONTENTS + numArtwork, artworkList);
    }

    /**
     * s
     * 모든 작품 조회(인덱스랑 url만)
     */
    public DefaultRes<List<ArtworkMini>> findAllIndexAndUrl(final int a_idx) {
        List<ArtworkMini> artworkMiniList = artworkMapper.findAllIndexAndUrl(a_idx);
        final int numArtwork = artworkMapper.findRealAll().size();
        for (ArtworkMini artworkMini : artworkMiniList) {
            artworkMini.setPic_url(artworkPicMapper.findByArtIdx(artworkMini.getA_idx()).getPic_url());
        }
        return DefaultRes.res(StatusCode.OK, ResponseMessage.READ_ALL_CONTENTS + numArtwork, artworkMiniList);
    }


    /**
     * 모든 작품 조회 - ios
     *
     * @return DefaultRes
     */
    public DefaultRes<List<Artwork>> findAllIos() {
        List<Artwork> artworkList = artworkMapper.findAllIos();
        for (Artwork artwork : artworkList) {
            artwork.setPic_url(artworkPicMapper.findByArtIdx(artwork.getA_idx()).getPic_url());
        }
        return DefaultRes.res(StatusCode.OK, ResponseMessage.READ_ALL_CONTENTS, artworkList);
    }

    /**
     * 모든 작품 조회(인덱스랑 url만) - ios
     *
     */
    public DefaultRes<List<ArtworkMini>> findAllIndexAndUrlIos(){
        List<ArtworkMini> artworkMiniList = artworkMapper.findAllIndexAndUrlIos();
        for (ArtworkMini artworkMini: artworkMiniList){
            artworkMini.setPic_url(artworkPicMapper.findByArtIdx(artworkMini.getA_idx()).getPic_url());
        }
        return DefaultRes.res(StatusCode.OK, ResponseMessage.READ_ALL_CONTENTS, artworkMiniList);
    }





    /**
     * 작품 인덱스로 조회
     *
     * @param a_idx 작품 인덱스
     * @return DefaultRes
     */
    public DefaultRes<Artwork> findByArtIdx(final int a_idx) {
        Artwork artwork = artworkMapper.findByIdx(a_idx);
        if (artwork == null) {
            return DefaultRes.res(StatusCode.FORBIDDEN, ResponseMessage.NOT_FOUND_CONTENT);
        }
        artwork.setPic_url(artworkPicMapper.findByArtIdx(artwork.getA_idx()).getPic_url());
        return DefaultRes.res(StatusCode.OK, ResponseMessage.READ_CONTENT, artwork);
    }

    /**
     * 작품 인덱스로 조회
     *
     * @param a_idx 작품 인덱스
     * @return DefaultRes <ArtworkRes>
     */
    public DefaultRes<ArtworkRes> findByArtworkIdx(final int a_idx, final int u_idx) {
        Artwork artwork = artworkMapper.findByIdx(a_idx);
        //log.info(artwork.toString());
        if (artwork == null) {
            return DefaultRes.res(StatusCode.FORBIDDEN, ResponseMessage.NOT_FOUND_ARTWORK);
        }
        User user = userMapper.findByUidx(artwork.getU_idx());
        if (user == null) {
            return DefaultRes.res(StatusCode.NOT_FOUND, ResponseMessage.NOT_FOUND_USER);
        }
        try {
            ArtworkRes artworkRes = new ArtworkRes(artwork, user);
            artworkRes.setPic_url(artworkPicMapper.findByArtIdx(artwork.getA_idx()).getPic_url());
            artworkRes.setA_size(artworkRes.getA_depth() * artworkRes.getA_height() * artworkRes.getA_width());
            artworkRes.setIslike(checkLike(u_idx, a_idx));
            // 판매불가 (p_state: 0)
            if (artwork.getA_purchaseState() == 0) {
                artworkRes.setA_purchaseState(0);
            }
            // 구매가능 (p_state: 1,2,3)
            else if (artwork.getA_purchaseState() < 10) {
                artworkRes.setA_purchaseState(1);
            }
            // 판매완료 (p_state: 11,12,13)
            else if (artwork.getA_purchaseState() > 10) {
                artworkRes.setA_purchaseState(11);
            }
            return DefaultRes.res(StatusCode.OK, ResponseMessage.READ_CONTENT, artworkRes);
        } catch (Exception e) {
            log.error(e.getMessage());
            return DefaultRes.res(StatusCode.INTERNAL_SERVER_ERROR, ResponseMessage.INTERNAL_SERVER_ERROR);
        }

    }

    /**
     * 작품 인덱스로 artworklist 조회
     *
     * @param a_idx 작품 인덱스
     * @return DefaultRes
     */
    public DefaultRes<List<ArtworkLike>> findAllLikesByArtIdx(final int a_idx) {
        List<ArtworkLike> likeList = artworkLikeMapper.findArtworkLikeByArtIdx(a_idx);
        if (likeList == null) {
            return DefaultRes.res(StatusCode.NOT_FOUND, ResponseMessage.NO_ARTWORKLIKE);
        }
        return DefaultRes.res(StatusCode.OK, ResponseMessage.READ_ALL_ARTWORKLIKE, likeList);
    }

    public DefaultRes<Integer> getLikecountByArtIdx(final int a_idx) {
        Artwork artwork = artworkMapper.findByIdx(a_idx);
        if (artwork == null) {
            return DefaultRes.res(StatusCode.FORBIDDEN, ResponseMessage.NOT_FOUND_CONTENT);
        }
        return DefaultRes.res(StatusCode.OK, ResponseMessage.READ_CONTENT, artwork.getA_like_count());
    }

    @Transactional
    public DefaultRes saveArtworkLike(final int a_idx, final int u_idx) {
        Artwork artwork = findByArtIdx(a_idx).getData();
        try {
            if (artwork == null) {
                return DefaultRes.res(StatusCode.FORBIDDEN, ResponseMessage.NOT_FOUND_CONTENT);
            }
            ArtworkLike artworkLike = artworkLikeMapper.findByUserIdxAndArtworkIdx(u_idx, a_idx);
            if (artworkLike == null) {
                artworkMapper.like(a_idx, artwork.getA_like_count() + 1);
                Date date = new Date();
                SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd hh:mm:ss");
                artworkLikeMapper.save(u_idx, a_idx, sdf.format(date));
            } else {
                artworkMapper.like(a_idx, artwork.getA_like_count() - 1);
                artworkLikeMapper.deleteByUserIdxAndArtworkIdx(u_idx, a_idx);
            }
            artwork = findByArtIdx(a_idx).getData();
            artwork.setAuth(checkAuth(u_idx, a_idx));
            artwork.setIslike(checkLike(u_idx, a_idx));
            return DefaultRes.res(StatusCode.OK, ResponseMessage.LIKE_CONTENT, artwork);
        } catch (Exception e) {
            log.error(e.getMessage());
            TransactionAspectSupport.currentTransactionStatus().setRollbackOnly();
            return DefaultRes.res(StatusCode.DB_ERROR, ResponseMessage.DB_ERROR);
        }
    }

    /**
     * 작품 저장
     *
     * @param artworkReq 작품 데이터
     * @return DefaultRes
     */
    @Transactional
    public DefaultRes save(final ArtworkReq artworkReq) {
        if (artworkReq.checkProperties()) {
            try {
                log.info("artwork url: " + artworkReq.getPic_url().toString());
                if (artworkReq.getPic_url() == null) {
                    return DefaultRes.res(StatusCode.BAD_REQUEST, ResponseMessage.ARTWORK_NOPICUTRE);
                }
//                Date date = new Date();
                Calendar calendar = Calendar.getInstance();
                java.util.Date date = calendar.getTime();

                artworkReq.setA_date(date);
                artworkMapper.save(artworkReq);

                final int artIdx = artworkReq.getA_idx();
                artworkPicMapper.save(artIdx, s3FileUploadService.upload(artworkReq.getPic_url(), "artwork"));
                return DefaultRes.res(StatusCode.CREATED, ResponseMessage.CREATE_CONTENT);

            } catch (IOException e) {
                log.info(e.getMessage());
                TransactionAspectSupport.currentTransactionStatus().setRollbackOnly();
                return DefaultRes.res(StatusCode.DB_ERROR, ResponseMessage.DB_ERROR);
            }
        }
        return DefaultRes.res(StatusCode.BAD_REQUEST, ResponseMessage.FAIL_CREATE_CONTENT);
    }

    /**
     * 작품 데이터 수정
     *
     * @param artworkReq 작품 데이터
     * @return DefaultRes
     */
    @Transactional
    public DefaultRes update(final ArtworkReq artworkReq) {
        if (artworkReq.checkProperties()) {
            try {
                log.info("update artwork url: " + artworkReq.getPic_url().toString());
                log.info("file name: " + artworkReq.getPic_url().getOriginalFilename());
                if (artworkReq.getPic_url() == null) {
                    return DefaultRes.res(StatusCode.BAD_REQUEST, ResponseMessage.ARTWORK_NOPICUTRE);
                }
                Date date = new Date();
                artworkReq.setA_date(date);
                artworkReq.setA_active(true);
                artworkPicMapper.update(artworkReq.getA_idx(), s3FileUploadService.upload(artworkReq.getPic_url(), "artwork"));
                artworkMapper.updateByArtIdxReq(artworkReq, artworkReq.getA_idx());
                return DefaultRes.res(StatusCode.OK, ResponseMessage.UPDATE_CONTENT);
            } catch (Exception e) {
                log.error(e.getMessage());
                e.printStackTrace();
                TransactionAspectSupport.currentTransactionStatus().setRollbackOnly();
                return DefaultRes.res(StatusCode.DB_ERROR, ResponseMessage.DB_ERROR);
            }
        }
        return DefaultRes.res(StatusCode.BAD_REQUEST, ResponseMessage.FAIL_UPDATE_CONTENT);
    }

    /**
     * 컨텐츠 삭제
     *
     * @param artIdx
     * @return DefaultRes
     */
    @Transactional
    public DefaultRes deleteByArtIdx(final int artIdx) {
        try {
            if (purchaseMapper.findTransactionsByArtIdx(artIdx).isEmpty()) {
                artworkLikeMapper.deleteByArtIdx(artIdx);
                artworkPicMapper.deleteByArtIdx(artIdx);
                commentMapper.deleteByArtIdx(artIdx);
                DisplayContent displayContent = displayContentMapper.findByArtworkIdx(artIdx);
                if (displayContent != null) {
                    displayMapper.deleteByDisplayIdx(displayContent.getD_idx());
                }
                displayContentMapper.deleteByArtIdx(artIdx);
                artworkMapper.deleteByArtIdx(artIdx);
                return DefaultRes.res(StatusCode.NO_CONTENT, ResponseMessage.DELETE_CONTENT);
            } else {
                Artwork artwork = artworkMapper.findByIdx(artIdx);
                artwork.setA_active(false);
                artworkMapper.updateByArtIdx(artwork, artIdx);
                return DefaultRes.res(StatusCode.OK, ResponseMessage.UNCOMPLETED_PURCHASE);
            }
        } catch (Exception e) {
            log.error(e.getMessage());
            TransactionAspectSupport.currentTransactionStatus().setRollbackOnly();
            return DefaultRes.res(StatusCode.DB_ERROR, ResponseMessage.DB_ERROR);
        }
    }

    /**
     * 글 권한 확인
     *
     * @param userIdx 사용자 고유 번호
     * @param artIdx  글 고유 번호
     * @return boolean
     */
    public boolean checkAuth(final int userIdx, final int artIdx) {
        return userIdx == artworkMapper.findByIdx(artIdx).getU_idx();
    }

    /**
     * 구매할 작품에 대한 정보 GET
     *
     * @param a_idx
     * @return PurchaseProduct (상품 정보)
     */
    public DefaultRes<PurchaseProduct> getPurchaseArtworkInfo(final int a_idx) {
        try {
            if (artworkMapper.findByIdx(a_idx) == null) {
                return DefaultRes.res(StatusCode.BAD_REQUEST, ResponseMessage.ARTWORK_NOPICUTRE);
            }
            Artwork artwork = artworkMapper.findByIdx(a_idx); //작품
            User user = userMapper.findByUidx(artwork.getU_idx());  //작가 인덱스
            PurchaseProduct purchaseProduct = new PurchaseProduct();
            purchaseProduct.setArtistSchool(user.getU_school());
            purchaseProduct.setArtistName(user.getU_name());
            purchaseProduct.setArtworkName(artwork.getA_name());
            purchaseProduct.setArtworkPrice(artwork.getA_price()); // VAT를 제외한 가격
            purchaseProduct.setPurchaseState(artwork.getA_purchaseState());

            final int productSize = artwork.getA_size();
            if (artwork.getA_price() * (1.1) >= 150000) { // 배송비는 VAT를 붙인 후 계산
                purchaseProduct.setDeliveryCharge(0);
            } else if (productSize < 2412) {
                purchaseProduct.setDeliveryCharge(3000);
            } else if (productSize < 6609) {
                purchaseProduct.setDeliveryCharge(4000);
            } else {
                purchaseProduct.setDeliveryCharge(5000);
            }
            return DefaultRes.res(StatusCode.OK, ResponseMessage.READ_CONTENT, purchaseProduct);
        } catch (Exception e) {
            e.printStackTrace();
            log.error(e.getMessage());
            return DefaultRes.res(StatusCode.DB_ERROR, ResponseMessage.DB_ERROR);
        }
    }

    /**
     * 구매 Service - 구매 데이터 전달 + 가격 정보 올리기 + (1/7) artwork a_purchaseState update
     *
     * @param buyerIdx    구매자 고유번호
     * @param a_idx       작품 고유번호
     * @param purchaseReq 구매 정보
     * @return boolean
     */
    @Transactional
    public DefaultRes<PurchaseReq> purchaseArtwork(final int buyerIdx, final int a_idx, final PurchaseReq purchaseReq) {
        if (purchaseReq.checkPurchaseReq()) {
            try {
                //---------------------작품 데이터 저장--------------------
                //작품 정보
                final Artwork artwork = artworkMapper.findByIdx(a_idx);

                // 구매 중인(p_state < 30)  a_idx 가 하나라도 있으면 구매 불가
                List<Purchase> purchases_30 = purchaseMapper.findTransactionsByArtIdxState30(a_idx);// a_idx 중 30인 purchase
                if (!purchases_30.isEmpty()) {
                    return DefaultRes.res(StatusCode.NO_CONTENT, ResponseMessage.FAIL_CREATE_PURCHASE_ING);
                }

//                if((purchaseReq.isP_isPost() && (artwork.getA_purchaseState() == 1 || artwork.getA_purchaseState() == 3))
//                    ||(!purchaseReq.isP_isPost() && (artwork.getA_purchaseState() == 1 || artwork.getA_purchaseState() == 2))) {
//                  if(artwork.getA_purchaseState() == 1) {


                if (artwork.getA_purchaseState() == 1) {
                    if (!purchaseReq.isP_isPost()) {
                        purchaseReq.setP_address("");
                        purchaseReq.setP_phone("");
                        purchaseReq.setP_recipient("");
                    }
                    final int artistIdx = artwork.getU_idx();
                    final int productSize = artwork.getA_size();
                    int purchasePrice = (int) (artwork.getA_price() * 1.1);
                    if (purchasePrice > 150000) {
                        purchasePrice += 0;
                    } else if (productSize < 2412) {
                        purchasePrice += 3000;
                    } else if (productSize < 6609) {
                        purchasePrice += 4000;
                    } else {
                        purchasePrice += 5000;
                    }

                    if (purchaseReq.isP_isPost()) { //상태
                        purchaseReq.setP_state(20);
                    } else {
                        purchaseReq.setP_state(10);
                    }
                    Calendar calendar = Calendar.getInstance(); // 시간
                    java.util.Date date = calendar.getTime();
                    purchaseReq.setP_currentTime(date);

                    purchaseReq.setA_idx(a_idx); // a_idx
                    purchaseReq.setP_sellerIdx(artistIdx);
                    purchaseReq.setP_buyerIdx(buyerIdx);
                    purchaseReq.setP_price(purchasePrice); // VAT와 배송비까지 포함한 최종가격

                    // 구매 테이블에 추가
                    purchaseMapper.savePurchaseData(purchaseReq);
                    // 아트워크 구매 상태 변경 1|2|3. -> 11|12|13
                    int a_purchaseState = artwork.getA_purchaseState() + 10;
                    artworkMapper.updatePurchaseStateByAIdx(a_purchaseState, artwork.getA_idx());

                    return DefaultRes.res(StatusCode.OK, ResponseMessage.CREATE_PURCHASE, purchaseReq);
                }
                return DefaultRes.res(StatusCode.BAD_REQUEST, ResponseMessage.UNAUTHORIZED_WAY);
            } catch (Exception e) {
                log.error(e.getMessage());
                e.printStackTrace();
                TransactionAspectSupport.currentTransactionStatus().setRollbackOnly();
                return DefaultRes.res(StatusCode.DB_ERROR, ResponseMessage.DB_ERROR);
            }
        }
        return DefaultRes.res(StatusCode.BAD_REQUEST, ResponseMessage.NOT_FOUND_PURCHASE);
    }

    public boolean checkLike(final int userIdx, final int artworkIdx) {
        return artworkLikeMapper.findByUserIdxAndArtworkIdx(userIdx, artworkIdx) != null;
    }


    /**
     * size, form, category, keyword 를 이용하여 작품 필터
     *
     * @param artworkFilterReq
     * @return Artwork
     */
    @Transactional
    public DefaultRes filterArtworkPic(final ArtworkFilterReq artworkFilterReq, final int artIdx) {
        try {
            List<ArtworkPic> artworkPicList = new ArrayList<>();
            List<Integer> artworkIdxList = new ArrayList<>();

            String size = artworkFilterReq.getA_size();
            String form = artworkFilterReq.getA_form();
            String category = artworkFilterReq.getA_category();
            String keyword = artworkFilterReq.getA_keyword();

            log.info(size + " " + form + " " + category + " " + keyword);
//            artworkIdxList = artworkMapper.findArtIdxBySize(0, 100000);
            if (!size.equals("")) {
                if (artIdx == -1) {
                    switch (size) {
                        case "S":
                            artworkIdxList = artworkMapper.findArtIdxBySize(0, 2411); //사이즈가 S인 a_idx List
                            break;
                        case "M":
                            artworkIdxList = artworkMapper.findArtIdxBySize(2412, 6608); //사이즈가 M인 a_idx List
                            break;
                        case "L":
                            artworkIdxList = artworkMapper.findArtIdxBySize(6609, 10628); //사이즈가 L인 a_idx List
                            break;
                        case "XL":
                            artworkIdxList = artworkMapper.findArtIdxBySize(10629, 21134); //사이즈가 XL인 a_idx List
                            break;
                    }
                } else {
                    switch (size) {
                        case "S":
                            artworkIdxList = artworkMapper.findArtIdxBySizeBelowIdx(0, 2411, artIdx); //사이즈가 S인 a_idx List
                            break;
                        case "M":
                            artworkIdxList = artworkMapper.findArtIdxBySizeBelowIdx(2412, 6608, artIdx); //사이즈가 M인 a_idx List
                            break;
                        case "L":
                            artworkIdxList = artworkMapper.findArtIdxBySizeBelowIdx(6609, 10628, artIdx); //사이즈가 L인 a_idx List
                            break;
                        case "XL":
                            artworkIdxList = artworkMapper.findArtIdxBySizeBelowIdx(10629, 21134, artIdx); //사이즈가 XL인 a_idx List
                            break;
                    }
                }
            }

            if (artIdx == -1) {
                if (!form.equals("")) {
                    List<Integer> artworkIdxByFormList = artworkMapper.findArtIdxByForm(form);
                    if (artworkIdxList.size() != 0) {
                        artworkIdxList.retainAll(artworkIdxByFormList); //artworkIdxList에서 artworkIdxByFormList와  공통 요소만 저장 공통 요소만 저장
                    } else {
                        artworkIdxList.addAll(artworkIdxByFormList);
                    }
                }
                if (!category.equals("")) {
                    List<Integer> artworkIdxByCategoryList = artworkMapper.findArtIdxByCategory(category);
                    if (artworkIdxList.size() != 0) {
                        artworkIdxList.retainAll(artworkIdxByCategoryList);//artworkIdxList에서 artworkIdxByCategoryList와 공통 요소만 저장
                    } else {
                        artworkIdxList.addAll(artworkIdxByCategoryList);
                    }
                }
                if (!keyword.equals("")) {
                    String likeKeyword = '%' + keyword + '%';
                    List<Integer> artworkIdxByKeywordList = artworkMapper.findArtIdxByKeyword(keyword, likeKeyword);
                    if (artworkIdxList.size() != 0) {
                        artworkIdxList.retainAll(artworkIdxByKeywordList);//artworkIdxList에서 artworkIdxByKeywordList 공통 요소만 저장
                    } else {
                        artworkIdxList.addAll(artworkIdxByKeywordList);
                    }
                }
                if (size.equals("") && form.equals("") && category.equals("") && keyword.equals("")) {
                    artworkIdxList = artworkMapper.findAllArtIdx();
                }

                for (int a_idx : artworkIdxList) {
                    artworkPicList.add(artworkPicMapper.findByArtIdx(a_idx));
                }
            } else {
                if (!form.equals("")) {
                    List<Integer> artworkIdxByFormList = artworkMapper.findArtIdxByFormBelowIdx(form, artIdx);
                    if (artworkIdxList.size() != 0) {
                        artworkIdxList.retainAll(artworkIdxByFormList); //artworkIdxList에서 artworkIdxByFormList와  공통 요소만 저장 공통 요소만 저장
                    } else {
                        artworkIdxList.addAll(artworkIdxByFormList);
                    }
                }
                if (!category.equals("")) {
                    List<Integer> artworkIdxByCategoryList = artworkMapper.findArtIdxByCategoryBelowIdx(category, artIdx);
                    if (artworkIdxList.size() != 0) {
                        artworkIdxList.retainAll(artworkIdxByCategoryList);//artworkIdxList에서 artworkIdxByCategoryList와 공통 요소만 저장
                    } else {
                        artworkIdxList.addAll(artworkIdxByCategoryList);
                    }
                }
                if (!keyword.equals("")) {
                    String likeKeyword = '%' + keyword + '%';
                    List<Integer> artworkIdxByKeywordList = artworkMapper.findArtIdxByKeywordBelowIdx(keyword, likeKeyword, artIdx);
                    if (artworkIdxList.size() != 0) {
                        artworkIdxList.retainAll(artworkIdxByKeywordList);//artworkIdxList에서 artworkIdxByKeywordList 공통 요소만 저장
                    } else {
                        artworkIdxList.addAll(artworkIdxByKeywordList);
                    }
                }
                if (size.equals("") && form.equals("") && category.equals("") && keyword.equals("")) {
                    artworkIdxList = artworkMapper.findAllArtIdxBelow(artIdx);
                }

                for (int a_idx : artworkIdxList) {
                    artworkPicList.add(artworkPicMapper.findByArtIdx(a_idx));
                }
            }

            if (artworkPicList.isEmpty()) {
                return DefaultRes.res(StatusCode.NO_CONTENT, ResponseMessage.NOT_FOUND_CONTENT, artworkPicList);
            }

            List sublist = new ArrayList();
            if (artworkPicList.size() > 15) {
                sublist = artworkPicList.subList(0, 15);
            } else {
                sublist = artworkPicList.subList(0, artworkPicList.size());
            }

            int total = artworkIdxList.size();
            return DefaultRes.res(StatusCode.OK, ResponseMessage.READ_ALL_CONTENTS, sublist, total);

        } catch (Exception e) {
            log.error(e.getMessage());
            return DefaultRes.res(StatusCode.DB_ERROR, ResponseMessage.DB_ERROR);
        }
    }


    /**
     * size, form, category, keyword 를 이용하여 작품 필터 - ios
     * @param artworkFilterReq
     * @return Artwork
     */
    @Transactional
    public DefaultRes filterArtworkPicIos(final ArtworkFilterReq artworkFilterReq){
        try{
            List<ArtworkPic> artworkPicList = new ArrayList<>();
            List<Integer> artworkIdxList = new ArrayList<>();

            String size = artworkFilterReq.getA_size();
            String form = artworkFilterReq.getA_form();
            String category = artworkFilterReq.getA_category();
            String keyword = artworkFilterReq.getA_keyword();

            log.info(size + " " +form + " " +  category + " " + keyword);
//            artworkIdxList = artworkMapper.findArtIdxBySize(0, 100000);
            if(!size.equals("")) {
                switch (size) {
                    case "S":
                        artworkIdxList = artworkMapper.findArtIdxBySize(0, 2411); //사이즈가 S인 a_idx List
                        break;
                    case "M":
                        artworkIdxList = artworkMapper.findArtIdxBySize(2412, 6608); //사이즈가 M인 a_idx List
                        break;
                    case "L":
                        artworkIdxList = artworkMapper.findArtIdxBySize(6609, 10628); //사이즈가 L인 a_idx List
                        break;
                    case "XL":
                        artworkIdxList = artworkMapper.findArtIdxBySize(10629, 21134); //사이즈가 XL인 a_idx List
                        break;
                }
            }
            if(!form.equals("")){
                List<Integer> artworkIdxByFormList = artworkMapper.findArtIdxByForm(form);
                if(artworkIdxList.size() != 0) {
                    artworkIdxList.retainAll(artworkIdxByFormList); //artworkIdxList에서 artworkIdxByFormList와  공통 요소만 저장 공통 요소만 저장
                }else{
                    artworkIdxList.addAll(artworkIdxByFormList);
                }
            }
            if(!category.equals("")){
                List<Integer> artworkIdxByCategoryList = artworkMapper.findArtIdxByCategory(category);
                if(artworkIdxList.size() != 0){
                    artworkIdxList.retainAll(artworkIdxByCategoryList);//artworkIdxList에서 artworkIdxByCategoryList와 공통 요소만 저장
                }else{
                    artworkIdxList.addAll(artworkIdxByCategoryList);
                }
            }
            if(!keyword.equals("")){
                String likeKeyword = '%'+keyword+'%';
                List<Integer> artworkIdxByKeywordList = artworkMapper.findArtIdxByKeyword(keyword, likeKeyword);
                if(artworkIdxList.size() != 0){
                    artworkIdxList.retainAll(artworkIdxByKeywordList);//artworkIdxList에서 artworkIdxByKeywordList 공통 요소만 저장
                }else{
                    artworkIdxList.addAll(artworkIdxByKeywordList);
                }
            }
            if(size.equals("") && form.equals("") && category.equals("") && keyword.equals("")){
                artworkIdxList = artworkMapper.findAllArtIdx();
            }

            for(int a_idx : artworkIdxList){
                artworkPicList.add(artworkPicMapper.findByArtIdx(a_idx));
            }
            if(artworkPicList.isEmpty()){
                return DefaultRes.res(StatusCode.NO_CONTENT, ResponseMessage.NOT_FOUND_CONTENT, artworkPicList);
            }

            return DefaultRes.res(StatusCode.OK, ResponseMessage.READ_ALL_CONTENTS, artworkPicList);

        }catch (Exception e){
            log.error(e.getMessage());
            return DefaultRes.res(StatusCode.DB_ERROR, ResponseMessage.DB_ERROR);
        }
    }



}
