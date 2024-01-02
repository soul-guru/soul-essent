use summarizer::{summarize,par_summarize} ;
use std::fs as fs ;

fn main() {
    let text: String = fs::read_to_string("wiki.txt").expect( "Could not read wiki.txt" );

    let reduction_factor: f32 = 0.4 ;

    // Use summarize of par_summarize here
    let summary: String = summarize( text.as_str() , reduction_factor ) ;

    println!( "Summary is {}" , summary ) ;
}