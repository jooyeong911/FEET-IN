<template>
  <div class="mt-5 container review-container">
    <div class="review-title text-center mt-5">
      <h3 class="position-relative d-inline-block">후기 게시판</h3>
      <br>
      <h2 class="position-relative d-inline-block">Reviews</h2>
    </div>

    <!-- TODO: 페이지 바 시작 -->
    <div class="col-md-12" v-if="false">
      <div class="md-3">
        Items per Page :
        <select v-model="pageSize" @change="handlePageSizeChange($event)">
          <!-- pageSizes : [3,6,9] -->
          <option v-for="size in pageSizes" :key="size" :value="size">
            {{ size }}
          </option>
        </select>
      </div>
    </div>

    <div style="clear: both"></div>
    <div class="card-wrapper" v-for="(board, index) in boards" :key="index">
      <div class="card mt-1">
        <div class="card-body profile-container">
          <div class="row">
            <div class="col-md-2 text-center pt-3 dt-profile-icon-container">
              <img :src="board.url2" id="product-img" />
            </div>
            <div class="col-md-5 mt-3">
              <div class="float-left user-name">{{ board.writer }}</div>
              <!-- 별점추가 -->
              <span v-for="n in board.starNum" :key="n"><img class="imgStar" style="height:20px;width:20px;" src="../../assets/images/staricon.png"></span>
              <div class="mt-2"></div>
              <a class="review-content-title" @click="toogleShow(index)" style="cursor:pointer;">
                {{ board.title }}
              </a>
              <div class="review mt-2" v-if="!board.type">
                <div>
                  {{ board.content }}
                </div>
                <div>
                  <img :src="board.url" id="review-img"  class="mt-2"/>
                </div>
                <div class="mt-5 re-btn">
                  <a class="btn text-black" @click="deleteBoard(board.reviewId)" style="float: right">삭제</a>
                </div>
              </div>
              <div>{{ board.insertTime }}</div>
            </div>
          </div>
        </div>
      </div>
    </div>
    <b-pagination
      class="mt-2"
      v-model="page"
      :total-rows="count"
      :per-page="pageSize"
      align="center"
      @change="handlePageChange"
    >
    </b-pagination>
  </div>
</template>

<script>
import UploadService from "@/services/review/UploadFilesService";
import review from "@/assets/js/review";

/* eslint-disable */
export default {
  name: "boards",
  // 변수를 초기화 하기
  data() {
    return {
      boards: [],
      isShow1: true,
      // Todo: title => searchEmail 변경 수정
      searchTitle: "",
      // Todo:아래 변수 추가
      page: 1,
      count: 0,
      pageSize: 9,
      pageSizes: [9],

      userName: this.$store.state.auth.user,
    };
  },
  methods: {
    getRequestParams(searchTitle, page, pageSize) {
      let params = {};

      if (searchTitle) {
        params["searchTitle"] = searchTitle;
      }

      if (page) {
        params["page"] = page - 1;
      }

      if (pageSize) {
        params["size"] = pageSize;
      }
      return params;
    },

    toogleShow(index) {
      this.boards[index].type = !this.boards[index].type;
    },

    deleteBoard(id) {
      if(confirm("후기를 삭제하시겠습니까?")) {
        UploadService.delete(id)
            .then(() => {
              this.retrieveBoards();
            })
            .catch((e) => {
              alert(e);
            });
      }
    },

    retrieveBoards() {
      const params = this.getRequestParams(
        this.searchTitle,
        this.page,
        this.pageSize
      );
      UploadService.getFiles(params)
        // 성공하면 then에 들어옴(객체, 응답메세지)
        .then((response) => {
          this.boards = response.data;
          this.count = this.boards[0].totalItems;
        });
    },

    handlePageChange(value) {
      this.page = value;
      this.retrieveBoards();
    },
    handlePageSizeChange(event) {
      this.pageSize = event.target.value; //select 박스의 값 (3,6,9)
      this.page = 1;
      this.retrieveBoards();
    },

  },
  mounted() {
    this.retrieveBoards();
    review();
  },
};
</script>

<style scoped>
@import "@/assets/css/review.css";
@import "@/assets/css/page.css";
</style>
