import { Component, Input, OnInit } from '@angular/core';
import { CommonModule } from '@angular/common';
import { RouterModule } from '@angular/router';
import { NotificationService } from '../../../core/services/notification';

@Component({
  selector: 'app-navbar',
  standalone: true,
  imports: [CommonModule, RouterModule],
  templateUrl: './navbar.html',
  styleUrls: ['./navbar.css']
})
export class NavbarComponent implements OnInit {
  @Input() pageTitle: string = '';
  unreadCount = 0;
  notifications: any[] = [];
  currentDate = new Date();
  constructor(private notificationService: NotificationService) {}

  ngOnInit() {
    this.notificationService.unreadCount$.subscribe(c => this.unreadCount = c);
    this.notificationService.dropdownList$.subscribe(data => this.notifications = data);
    setInterval(() => {
      this.currentDate = new Date();
    }, 60000);
  }

  onNotificationClick(requestId: string) {
    this.notificationService.markAsRead(requestId);
  }
}