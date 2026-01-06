import { Component, OnInit } from '@angular/core';
import { CommonModule } from '@angular/common';
import { Router } from '@angular/router';

@Component({
  selector: 'app-connection-success',
  standalone: true,
  imports: [CommonModule],
  templateUrl: './connection-success.html',
  styleUrls: ['./connection-success.css']
})
export class ConnectionSuccessComponent implements OnInit {
  constructor(private router: Router) {}
  ngOnInit(): void {
    setTimeout(() => {
      this.router.navigate(['/consumer/connections']);
    }, 3000);
  }
}