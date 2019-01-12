package org.sopt.artoo.service;

import lombok.extern.slf4j.Slf4j;
import org.sopt.artoo.dto.*;

import org.sopt.artoo.mapper.ArtworkMapper;
import org.sopt.artoo.mapper.ArtworkPicMapper;
import org.sopt.artoo.mapper.HomeMapper;
import org.sopt.artoo.mapper.UserMapper;
import org.sopt.artoo.model.DefaultRes;
import org.sopt.artoo.utils.ResponseMessage;
import org.sopt.artoo.utils.StatusCode;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.text.SimpleDateFormat;
import java.util.*;


@Slf4j
@Service
public class HomeService {

    private final HomeMapper homeMapper;
    private final ArtworkMapper artworkMapper;
    private final ArtworkPicMapper artworkPicMapper;


    /**
     * HomeMapper 생성자 의존성 주입
     */
    public HomeService(HomeMapper homeMapper, ArtworkMapper artworkMapper, ArtworkPicMapper artworkPicMapper) {
        this.homeMapper = homeMapper;
        this.artworkMapper = artworkMapper;
        this.artworkPicMapper = artworkPicMapper;
    }

    /**
     * 좋아요 순위 5개 작가, 작가 작품
     * @return DefaultRes
     */
    public DefaultRes getAllTodayContents(){
//        SimpleDateFormat mSimpleDateFormat = new SimpleDateFormat ( "yyyy-MM", Locale.KOREA );
        SimpleDateFormat mSimpleYearFormat = new SimpleDateFormat ( "yyyy", Locale.KOREA ); // 해당 연도
        SimpleDateFormat mSimpleMonthFormat = new SimpleDateFormat ( "MM", Locale.KOREA );  // 해당 월
        Date currentTime = new Date();  //그 날의 날짜 데이터

        String month = mSimpleMonthFormat.format(currentTime);
        String year = mSimpleYearFormat.format(currentTime);
        final List<Integer> todayUserIdxList = homeMapper.findTodayUserIdx(month, year); //오늘의 작가 u_idx 리스트(좋아요 순)
        List<Home> todayArtistList = new ArrayList<>();
        try {
            if(todayUserIdxList.size() < 5){         //좋아요 눌린 작가 수가 5명 이하일 때
                List<Integer> artistIdxList = artworkMapper.findAllUserIdx();
                artistIdxList.removeAll(todayUserIdxList); // 있는 userIdx 삭제
                for(int i = 0; todayUserIdxList.size() < 5; i++){
                    todayUserIdxList.add(artistIdxList.get(i));
                }
            }

            for (int i = 0; i < todayUserIdxList.size(); i++) {

                List<HomeData> artDataList = homeMapper.findArtistContentsByUserIdx(todayUserIdxList.get(i)); //u_name, u_year 리스트, pic_url은 null
                for (HomeData artData : artDataList) {
                    artData.setPic_url(artworkPicMapper.findByArtIdx(artData.getA_idx()).getPic_url());// artData의 pic_url 설정
                }
                Home todayArtist = new Home();
                todayArtist.setU_idx(todayUserIdxList.get(i));
                todayArtist.setU_name(homeMapper.findArtistNameDescriptByUserIdx(todayUserIdxList.get(i)).getU_name());
                todayArtist.setU_school(homeMapper.findArtistNameDescriptByUserIdx(todayUserIdxList.get(i)).getU_school());
                todayArtist.setU_description(homeMapper.findArtistNameDescriptByUserIdx(todayUserIdxList.get(i)).getU_description());
                todayArtist.setList(artDataList);

                todayArtistList.add(todayArtist);
            }


        }catch (Exception e){
            return DefaultRes.res(StatusCode.DB_ERROR, ResponseMessage.NOT_FOUND_PICTURES);
        }
        if(todayArtistList.isEmpty()) {
            return DefaultRes.res(StatusCode.NOT_FOUND, ResponseMessage.NOT_FOUND_CONTENT);
        }
        return DefaultRes.res(StatusCode.OK, ResponseMessage.READ_ARTIST, todayArtistList);
    }


    /**
     *  테마 첫 화면 tag 테이블 넘겨주기
     * @return DefaultRes
     */
    public DefaultRes getAllTagInfo(){
        final List<Tag> themeList = homeMapper.findAllTag(); //모든 tag리스트 받아옴
        final Tag tag = themeList.get(0); //themeList 첫번째 Tag정보
        final List<ArtworkPic> themePicList = new ArrayList<>();
        List<Artwork> artworkList = artworkMapper.findTagsArtworkIdx(); //a_tag와 a_idx 갖고옴
        for (Artwork artwork : artworkList) {
            if (themePicList.size() == 6) {
                break;
            }
            String a_tags = artwork.getA_tags();
            String[] tagNum = a_tags.split(","); // {3,4,5}
            for (String aTagNum : tagNum) {
                if (String.valueOf(tag.getT_idx()).equals(aTagNum)) { //0번째 tag index와 aTagNum가 같으면
                    themePicList.add(artworkPicMapper.findByArtIdx(artwork.getA_idx())); //사진 가져오기
                }
            }
        }
        tag.setList(themePicList);
        themeList.set(0, tag);

        if(themeList.isEmpty()){
            return DefaultRes.res(StatusCode.NOT_FOUND, ResponseMessage.NOT_FOUND_CONTENT);
        }
        return DefaultRes.res(StatusCode.OK, ResponseMessage.READ_ALL_CONTENTS, themeList);
    }



    public DefaultRes<List<ArtworkPic>> getAllTagPicUrl(final int t_idx){
        final List<ArtworkPic> themePicList = new ArrayList<>();
        List<Artwork> artworkList = artworkMapper.findTagsArtworkIdx();
        for(Artwork artwork : artworkList){
            String a_tags = artwork.getA_tags();
            String[] tagNum = a_tags.split(","); // {3,4,5}
            for (String aTagNum : tagNum) {
                if (String.valueOf(t_idx).equals(aTagNum)) { //tag index와 aTagNum가 같으면
                    if(artworkPicMapper.findByArtIdx(artwork.getA_idx())!=null){
                        themePicList.add(artworkPicMapper.findByArtIdx(artwork.getA_idx())); //사진 가져오기
                    }
                }
            }
            if(themePicList.size() == 48){
                break;
            }
        }

        return DefaultRes.res(StatusCode.OK, ResponseMessage.READ_ALL_CONTENTS, themePicList);
    }

}
