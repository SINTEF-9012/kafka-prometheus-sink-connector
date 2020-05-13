package main

import (
    "fmt"
    "log"
    "net/http"
    "io/ioutil"
    "encoding/json"
)

type Target struct {
  Targets []string
  Labels Label
}

type Label struct {
  Job string
}

func (t Target) toString() string {
  result := "{\"targets\":["
  for i, t := range t.Targets {
    if i>0 {result = result + ","}
    result = result + "\"" + t + "\""
  }
  result = result + "],\"labels\":{\"job\":\"" + t.Labels.Job + "\"}}"
  return result
}

func main() {
  http.HandleFunc("/targets", func(w http.ResponseWriter, r *http.Request) {
    if r.Method == http.MethodPost || r.Method == http.MethodPut {
      b, err := ioutil.ReadAll(r.Body)
      if err != nil {
        panic(err)
      }
      var target Target
      json.Unmarshal(b, &target)
      fmt.Print(string(b))
      fmt.Fprintf(w, "Adding target %s", target.toString())
      return
    }
    if r.Method == http.MethodDelete {
      b, err := ioutil.ReadAll(r.Body)
      if err != nil {
        panic(err)
      }
      var target Target
      json.Unmarshal(b, &target)
      fmt.Fprintf(w, "Deleting target %s", target.toString())
      return
    }
    fmt.Fprintf(w, "Method %s not allowed!", r.Method)
    return
  })

  log.Fatal(http.ListenAndServe(":8080", nil))
}
