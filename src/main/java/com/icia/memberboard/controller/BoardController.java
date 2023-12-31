package com.icia.memberboard.controller;

import com.icia.memberboard.dto.BoardDTO;
import com.icia.memberboard.dto.CommentDTO;
import com.icia.memberboard.service.BoardService;
import com.icia.memberboard.service.CommentService;
import com.icia.memberboard.service.MemberService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

import java.io.IOException;
import java.util.*;

@Controller
@RequiredArgsConstructor
@RequestMapping("/board")
public class BoardController {
    private final BoardService boardService;
    private final CommentService commentService;
    private final MemberService memberService;

    @GetMapping("/save")
    private String save(){
        return "boardPages/boardSave";
    }

    @PostMapping("/save")
    private String save(@ModelAttribute BoardDTO boardDTO,
                        @RequestParam("memberEmail") String memberEmail) {
        try{
            boardService.save(boardDTO, memberEmail);
            return "redirect:/board";
        }catch (IOException ioException){
            return "redirect:/board/save";
        }
    }

    @GetMapping()
    private String list(@RequestParam(value = "page", required = false, defaultValue = "1") int page,
                        @RequestParam(value = "query", required = false, defaultValue = "") String query,
                        @RequestParam(value = "type", required = false, defaultValue = "1") int type,
                        Model model){
        Page<BoardDTO> boardDTOPage = boardService.findAll(page,query,type);
        int blockLimit = 3;
        int startPage = (((int) (Math.ceil((double) page / blockLimit))) - 1) * blockLimit + 1;
        int endPage = ((startPage + blockLimit - 1) < boardDTOPage.getTotalPages()) ? startPage + blockLimit - 1 : boardDTOPage.getTotalPages();
        model.addAttribute("boardList", boardDTOPage);
        model.addAttribute("startPage", startPage);
        model.addAttribute("endPage", endPage);
        model.addAttribute("query", query);
        model.addAttribute("type",type);
        model.addAttribute("page",page);
        return "boardPages/boardList";
    }

    @GetMapping("/detail/{id}")
    private String detail(@PathVariable("id") Long id,
                          Model model){
        boardService.increaseHits(id);
        BoardDTO boardDTO = boardService.findById(id);
        model.addAttribute("board", boardDTO);
        List<CommentDTO> commentDTOList = commentService.findAllByBoardId(id);
        model.addAttribute("commentList", commentDTOList);
        return "boardPages/boardDetail";
    }

    @DeleteMapping("/delete/{id}")
    private ResponseEntity delete(@PathVariable("id") Long id,
                                  @RequestParam("writerEmail") String writerEmail,
                                  @RequestParam("password") String password){
        try{
            boolean result = memberService.checkPassword(writerEmail, password);
            if (result){
                boardService.delete(id);
                return new ResponseEntity<>(HttpStatus.OK);
            }else{
                return new ResponseEntity<>(HttpStatus.BAD_REQUEST);
            }
        }catch (NoSuchElementException noSuchElementException){
            return new ResponseEntity<>(HttpStatus.NOT_FOUND);
        }
    }

    @GetMapping("/update/{id}")
    private String update(@PathVariable("id") Long id,
                                  Model model){
        try{
            BoardDTO boardDTO = boardService.findById(id);
            List<String> originalFilenameList = boardDTO.getOriginalFilename();
            List<String> storedFilenameList = boardDTO.getStoredFilename();

            List<Map<String, String>> boardFileList = new ArrayList<>();
            for (int i = 0; i< originalFilenameList.size(); i++){
                Map<String, String> boardFile = new HashMap<>();
                boardFile.put("originalFilename", originalFilenameList.get(i));
                boardFile.put("storedFilename", storedFilenameList.get(i));
                boardFileList.add(boardFile);
            }
            model.addAttribute("board", boardDTO);
            model.addAttribute("boardFileList",boardFileList);
            return "boardPages/boardUpdate";
        }catch (NoSuchElementException noSuchElementException){
            return "redirect:/board/detail/"+id;
        }
    }

    @PostMapping("/update")
    private String update(@ModelAttribute BoardDTO boardDTO,
                        @RequestParam(value = "deleteFileList", required = false, defaultValue = "") List<String> deleteFileList){
        try {
            System.out.println("컨트롤러 들어오나");
            boardService.update(boardDTO, deleteFileList);
            return "redirect:/board/detail/"+boardDTO.getId();
        } catch (IOException e) {
            return "redirect:/board/update/"+boardDTO.getId();
        }
    }
}

